package de.pocketcloud.cloud.template;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.template.TemplateCreateEvent;
import de.pocketcloud.cloud.event.impl.template.TemplateEditEvent;
import de.pocketcloud.cloud.event.impl.template.TemplateRemoveEvent;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.cloud.network.packet.impl.TemplateSyncPacket;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.CloudServerManager;
import de.pocketcloud.cloud.template.group.ServerGroupManager;
import de.pocketcloud.cloud.template.util.TemplateEditData;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.benchmark.BenchmarkTiming;
import de.pocketcloud.common.util.NumberUtils;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class TemplateManager implements Tickable, Loadable {

    @Getter
    @Accessors(fluent = true)
    private static TemplateManager instance = null;

    private final Map<String, Template> templates = new HashMap<>();

    public TemplateManager() {
        instance = this;
    }

    @Override
    public void load() {
        CloudLogger.get().info("Loading templates...");
        for (TemplateType type : TemplateType.values()) FileUtils.createDir(type.globalTemplatePath());
        CloudProvider.current().getTemplates()
                .thenSuccess(this.templates::putAll)
                .thenSuccess(_ -> ServerGroupManager.instance().load());
    }

    @Override
    public void unload() {
        CloudLogger.get().info("Unloading templates...");
        this.templates.clear();
        ServerGroupManager.instance().unload();
    }

    public void create(Template template) {
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
        if (template.settings().isMaintenance()) {
            template.players().forEach(player -> {
                if (template.settings().isLobby()) {
                    player.kick("MAINTENANCE");
                } else {
                    Optional<CloudServer> lobbyServer = CloudServerManager.instance().getFreeLobby();
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
        if (!ServerGroupManager.instance().isLoaded()) return;
        for (Template template : templates.values()) {
            if (template.settings().isAutoStart()) {
                int runningServers = CloudServerManager.instance().getAll(template).size();
                if (runningServers < template.settings().getMaxServerCount() && runningServers < template.settings().getMinServerCount()) {
                    if ((System.currentTimeMillis() - CloudServerManager.instance().lastServerStopTime()) >= 500) {
                        CloudServerManager.instance().start(template, template.settings().getMinServerCount() - runningServers);
                    }
                }
            }

            CloudServer latest = CloudServerManager.instance().getLatest(template).orElse(null);
            if (latest != null) {
                double requiredPercentage = template.settings().getStartNewPercentage();
                if (requiredPercentage <= 0) continue;
                int players = latest.playerCount();
                double percentage = (double) (100 * players) / latest.serverData().maxPlayers();
                if (percentage >= requiredPercentage && CloudServerManager.instance().checkCapacity(template)) {
                    CloudServerManager.instance().start(template, 1);
                }
            }
        }
    }

    public Optional<Template> get(String name) {
        return Optional.ofNullable(templates.getOrDefault(name, null));
    }

    public List<Template> getAll(TemplateType... types) {
        if (types.length > 0) return templates.values().stream()
                .filter(template -> {
                    for (TemplateType type : types) {
                        if (template.isTypeOf(type)) return true;
                    }
                    return false;
                }).toList();
        return templates.values().stream().toList();
    }
}