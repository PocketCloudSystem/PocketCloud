package de.pocketcloud.cloud.template;

import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.provider.ITemplateProvider;
import de.pocketcloud.api.search.SearchQuery;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.template.util.TemplateEditData;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.template.TemplateCreateEvent;
import de.pocketcloud.cloud.event.impl.template.TemplateEditEvent;
import de.pocketcloud.cloud.event.impl.template.TemplateRemoveEvent;
import de.pocketcloud.cloud.template.util.TemplateTypeHelper;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.cloud.network.packet.impl.TemplateSyncPacket;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.benchmark.BenchmarkTiming;
import de.pocketcloud.common.util.NumberUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public final class TemplateManager implements Tickable, Loadable, ITemplateProvider<Template> {

    private final Map<String, Template> templates = new HashMap<>();

    @Override
    public void load() {
        CloudLogger.get().info("Loading templates...");
        for (TemplateType type : TemplateType.values()) FileUtils.createDir(TemplateTypeHelper.globalTemplatePath(type));
        CloudProvider.current().getTemplates()
                .thenSuccess(this.templates::putAll)
                .thenSuccess(_ -> PocketCloud.instance().serverGroups().load());
    }

    @Override
    public void unload() {
        CloudLogger.get().info("Unloading templates...");
        this.templates.clear();
        PocketCloud.instance().serverGroups().unload();
    }

    public void add(Template template) {
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
            templates.put(template.name(), template);
            BenchmarkTiming res = Benchmark.stopTiming("template_creation");
            CloudLogger.get().success("Successfully §acreated §rthe template §b{}§r. §8(§rTook §b{}ms§8)", template.name(), NumberUtils.formatNumber(res.duration(), 2));
            TemplateSyncPacket.create(template, false).broadcastPacket();
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to create template {}", e, template.name());
            Benchmark.stopTiming("template_creation");
            templates.remove(template.name());
        }
    }

    public void edit(Template template, TemplateEditData editData) {
        Benchmark.startTiming("template_editing");
        TemplateEditEvent ev = new TemplateEditEvent(template, editData);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("template_editing");
            return;
        }

        editData.applyTo(template);
        CloudProvider.current().editTemplate(template, template.write());
        BenchmarkTiming res = Benchmark.stopTiming("template_editing");
        CloudLogger.get().success("Successfully §eedited §rthe template §b{}§r. §8(§rTook §b{}ms§8)", template.name(), NumberUtils.formatNumber(res.duration(), 2));
        TemplateSyncPacket.create(template, false).broadcastPacket();
        if (template.settings().maintenance()) {
            template.players().forEach(player -> {
                if (template.settings().lobby()) {
                    player.kick("MAINTENANCE");
                } else {
                    Optional<CloudServer> lobbyServer = PocketCloud.instance().servers().getFreeLobby();
                    if (lobbyServer.isEmpty()) player.kick("MAINTENANCE");
                    else player.transfer(lobbyServer.get());
                }
            });
        }
    }

    public void remove(Template template) {
        Benchmark.startTiming("template_removal");
        TemplateRemoveEvent ev = new TemplateRemoveEvent(template);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("template_removal");
            return;
        }

        CloudProvider.current().removeTemplate(template);
        if (Files.isDirectory(template.path())) {
            try {
                FileUtils.removeDirectory(template.path());
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to remove template directory of {}", e, template.name());
            }
        }

        templates.remove(template.name());
        BenchmarkTiming res = Benchmark.stopTiming("template_removal");
        CloudLogger.get().success("Successfully §cremoved §rthe template §b{}§r. §8(§rTook §b{}ms§8)", template.name(), NumberUtils.formatNumber(res.duration(), 2));
    }

    public boolean check(String name) {
        return templates.containsKey(name);
    }

    @Override
    public void tick(long currentTick) {
        if (!PocketCloud.instance().serverGroups().isLoaded()) return;
        for (Template template : templates.values()) {
            if (template.settings().autoStart()) {
                int runningServers = PocketCloud.instance().servers().query(ServerSearchQuery.create().ofTemplate(template)).size();
                if (runningServers < template.settings().maxServerCount() && runningServers < template.settings().minServerCount()) {
                    if ((System.currentTimeMillis() - PocketCloud.instance().servers().lastServerStopTime()) >= 500) {
                        PocketCloud.instance().servers().start(template, template.settings().minServerCount() - runningServers);
                    }
                }
            }

            CloudServer latest = PocketCloud.instance().servers().getLatest(template).orElse(null);
            if (latest != null) {
                double requiredPercentage = template.settings().startNewPercentage();
                if (requiredPercentage <= 0) continue;
                int players = latest.playerCount();
                double percentage = (double) (100 * players) / latest.data().maxPlayers();
                if (percentage >= requiredPercentage && PocketCloud.instance().servers().checkCapacity(template)) {
                    PocketCloud.instance().servers().start(template, 1);
                }
            }
        }
    }

    public Optional<Template> get(String name) {
        return Optional.ofNullable(templates.getOrDefault(name, null));
    }

    @Override
    public Collection<Template> query(SearchQuery<? extends ITemplate> searchQuery) {
        return filter(searchQuery);
    }

    @SuppressWarnings("unchecked")
    private <T extends ITemplate> Collection<Template> filter(SearchQuery<T> query) {
        return templates.values().stream()
                .filter(o -> query.matches((T) o))
                .toList();
    }

    @Override
    public Collection<Template> getAll() {
        return templates.values().stream().toList();
    }
}