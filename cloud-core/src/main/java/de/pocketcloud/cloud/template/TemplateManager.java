package de.pocketcloud.cloud.template;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.builder.ITemplateBuilder;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.api.provider.write.IWriteTemplateProvider;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.search.TemplateSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.util.TemplateEditData;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.builder.TemplateBuilder;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.template.TemplateCreateEvent;
import de.pocketcloud.cloud.event.impl.template.TemplateEditEvent;
import de.pocketcloud.cloud.event.impl.template.TemplateRemoveEvent;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.benchmark.BenchmarkTiming;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.util.NumberUtils;
import de.pocketcloud.shared.event.template.TemplateCreatedEvent;
import de.pocketcloud.shared.event.template.TemplateDeletedEvent;
import de.pocketcloud.shared.event.template.TemplateEditedEvent;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public final class TemplateManager implements Tickable, Loadable, IWriteTemplateProvider {

    private final Map<String, Template> templates = new HashMap<>();

    @Override
    public void load() {
        CloudLogger.get().info("Loading templates...");
        for (TemplateType type : TemplateType.values())
            FileUtils.createDir(TemplateTypeHelper.globalTemplatePath(type));
        CloudProvider.current().getTemplates()
                .thenSuccess(templates::putAll)
                .thenSuccess(_ -> PocketCloud.instance().serverGroups().load())
                .thenSuccess(_ -> {
                    for (Template template : templates.values()) {
                        if (template.settings().maxMemory() <= 0 && template.serverSoftware().download().realStartCommand().contains("{MAX_MEMORY}")) {
                            PocketCloud.instance().appendStartNotification("The setting §bmaxMemory §ccannot §rbe equal to §b0 §rfor template §b{}§r.", CloudLogLevel.WARN, template.name());
                            PocketCloud.instance().appendStartNotification("Setting it to §b1024M §rinstead...", CloudLogLevel.WARN, template.name());
                            template.settings().maxMemory(1024);
                            CloudProvider.current().editTemplate(template, template.write());
                        }
                    }
                });
    }

    @Override
    public void unload() {
        CloudLogger.get().info("Unloading templates...");
        this.templates.clear();
        PocketCloud.instance().serverGroups().unload();
    }

    @Override
    public void create(ITemplateBuilder builder) {
        Template template = (Template) builder.build();
        Benchmark.startTiming("template_creation");
        try {
            TemplateCreateEvent ev = new TemplateCreateEvent(template);
            ev.call();
            if (ev.isCancelled()) {
                Benchmark.stopTiming("template_creation");
                return;
            }

            CloudProvider.current().addTemplate(template);
            if (!Files.isDirectory(template.path())) Files.createDirectories(template.path());
            add(template);
            BenchmarkTiming res = Benchmark.stopTiming("template_creation");
            CloudLogger.get().success("Successfully §acreated §rthe template §b{}§r. §8(§rTook §b{}ms§8)", template.name(), NumberUtils.formatNumber(res.duration(), 2));
            template.syncOut();
            CloudAPI.instance().events().call(new TemplateCreatedEvent(template));
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to create template {}", e, template.name());
            Benchmark.stopTiming("template_creation");
            templates.remove(template.name());
        }
    }

    @Override
    @ApiStatus.Internal
    public void add(ITemplate template) {
        templates.put(template.name(), requireTemplate(template));
    }

    @Override
    public void edit(ITemplate template, TemplateEditData editData) {
        Template temp = requireTemplate(template);
        Benchmark.startTiming("template_editing");
        TemplateEditEvent ev = new TemplateEditEvent(temp, editData);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("template_editing");
            return;
        }

        editData.applyTo(temp);
        CloudProvider.current().editTemplate(temp, temp.write());
        BenchmarkTiming res = Benchmark.stopTiming("template_editing");
        CloudLogger.get().success("Successfully §eedited §rthe template §b{}§r. §8(§rTook §b{}ms§8)", temp.name(), NumberUtils.formatNumber(res.duration(), 2));
        temp.syncOut();
        CloudAPI.instance().events().call(new TemplateEditedEvent(template, editData));
        if (temp.settings().maintenance()) {
            temp.players().forEach(player -> {
                if (template.settings().lobby()) {
                    player.kick("MAINTENANCE");
                } else {
                    Optional<ICloudServer> lobbyServer = PocketCloud.instance().servers().getFreeLobby();
                    if (lobbyServer.isEmpty()) player.kick("MAINTENANCE");
                    else player.transfer(lobbyServer.get());
                }
            });
        }
    }

    @Override
    @ApiStatus.Internal
    public void remove(ITemplate template) {
        templates.remove(template.name());
    }

    @Override
    public void delete(ITemplate template) {
        Template temp = requireTemplate(template);
        Benchmark.startTiming("template_removal");
        TemplateRemoveEvent ev = new TemplateRemoveEvent(temp);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("template_removal");
            return;
        }

        PocketCloud.instance().servers().stop(template);
        CloudProvider.current().removeTemplate(temp);
        if (Files.isDirectory(temp.path())) {
            try {
                FileUtils.removeDirectory(temp.path());
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to remove template directory of {}", e, temp.name());
            }
        }

        remove(temp);
        BenchmarkTiming res = Benchmark.stopTiming("template_removal");
        CloudLogger.get().success("Successfully §cremoved §rthe template §b{}§r. §8(§rTook §b{}ms§8)", temp.name(), NumberUtils.formatNumber(res.duration(), 2));
        temp.markForRemoval().syncOut();
        CloudAPI.instance().events().call(new TemplateDeletedEvent(template));
    }

    @Override
    public ITemplateBuilder builder() {
        return new TemplateBuilder();
    }

    @Override
    public boolean check(String name) {
        return templates.containsKey(name);
    }

    @Override
    public ITemplate current() {
        throw new RuntimeException("There is no \"current\" template on the cloud side");
    }

    @Override
    public void tick(long currentTick) {
        if (!PocketCloud.instance().serverGroups().isLoaded()) return;
        for (ITemplate template : templates.values()) {
            if (template.settings().autoStart()) {
                int runningServers = PocketCloud.instance().servers().query(ServerSearchQuery.create().ofTemplate(template)).size();
                if (runningServers < template.settings().maxServerCount() && runningServers < template.settings().minServerCount()) {
                    if ((System.currentTimeMillis() - PocketCloud.instance().servers().lastServerStopTime()) >= 500) {
                        PocketCloud.instance().servers().start(template, template.settings().minServerCount() - runningServers);
                    }
                }
            }

            ICloudServer latest = PocketCloud.instance().servers().getLatest(template).orElse(null);
            if (latest != null) {
                double requiredPercentage = template.settings().startNewPercentage();
                if (requiredPercentage <= 0) continue;
                int players = latest.playerCount();
                double percentage = (double) players / latest.data().maxPlayers();
                if (percentage >= requiredPercentage && PocketCloud.instance().servers().checkCapacity(template)) {
                    PocketCloud.instance().servers().start(template, 1);
                }
            }
        }
    }

    @Override
    public Optional<ITemplate> get(String name) {
        return Optional.ofNullable(templates.get(name));
    }

    @Override
    public Collection<ITemplate> query(TemplateSearchQuery searchQuery) {
        return widen(templates.values().stream()
                .filter(searchQuery::matches)
                .toList());
    }

    @Override
    public Collection<ITemplate> query(Consumer<TemplateSearchQuery> queryConsumer) {
        TemplateSearchQuery searchQuery = new TemplateSearchQuery();
        queryConsumer.accept(searchQuery);
        return query(searchQuery);
    }

    @Override
    public int templateCount() {
        return templates.size();
    }

    @Override
    public Collection<ITemplate> getAll() {
        return widen(templates.values().stream().toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends ITemplate> Collection<ITemplate> widen(Collection<T> collection) {
        return (Collection<ITemplate>) collection;
    }

    private Template requireTemplate(ITemplate template) {
        if (!(template instanceof Template tmp)) {
            throw new IllegalArgumentException("Unsupported ICloudServer implementation: " + template.getClass().getName());
        }

        return tmp;
    }
}