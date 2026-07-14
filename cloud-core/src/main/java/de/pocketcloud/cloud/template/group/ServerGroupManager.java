package de.pocketcloud.cloud.template.group;

import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.provider.IServerGroupProvider;
import de.pocketcloud.api.search.SearchQuery;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.group.ServerGroupAddTemplateEvent;
import de.pocketcloud.cloud.event.impl.group.ServerGroupCreateEvent;
import de.pocketcloud.cloud.event.impl.group.ServerGroupRemoveEvent;
import de.pocketcloud.cloud.event.impl.group.ServerGroupRemoveTemplateEvent;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.benchmark.BenchmarkTiming;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.util.NumberUtils;
import de.pocketcloud.network.packet.impl.ServerGroupSyncPacket;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class ServerGroupManager implements Loadable, IServerGroupProvider<ServerGroup> {

    @Getter
    private boolean loaded = false;
    private final Map<String, ServerGroup> serverGroups = new HashMap<>();

    @Override
    public void load() {
        loaded = true;
        CloudLogger.get().info("Loading server groups...");
        CloudProvider.current().getServerGroups().thenSuccess(serverGroups::putAll);
    }

    @Override
    public void unload() {
        loaded = false;
        CloudLogger.get().info("Unloading server groups...");
        serverGroups.clear();
    }

    public void add(ServerGroup serverGroup) {
        Benchmark.startTiming("server_group_creation");
        try {
            ServerGroupCreateEvent ev = new ServerGroupCreateEvent(serverGroup);
            ev.call();
            if (ev.isCancelled()) {
                Benchmark.stopTiming("server_group_creation");
                return;
            }

            CloudProvider.current().addServerGroup(serverGroup);
            if (!Files.isDirectory(serverGroup.path())) Files.createDirectories(serverGroup.path());
            serverGroups.put(serverGroup.name(), serverGroup);
            BenchmarkTiming res = Benchmark.stopTiming("server_group_creation");
            CloudLogger.get().success("Successfully §acreated §rthe server group §b{}§r. §8(§rTook §b{}ms§8)", serverGroup.name(), NumberUtils.formatNumber(res.duration(), 2));
            ServerGroupSyncPacket.create(serverGroup, false).broadcast();
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to create server group {}", e, serverGroup.name());
            Benchmark.stopTiming("server_group_creation");
            serverGroups.remove(serverGroup.name());
        }
    }

    public void addTemplate(ServerGroup serverGroup, Template template) {
        Benchmark.startTiming("server_group_add_template");
        ServerGroupAddTemplateEvent ev = new ServerGroupAddTemplateEvent(serverGroup, template);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("server_group_add_template");
            return;
        }

        serverGroup.add(template);
        CloudProvider.current().editServerGroup(serverGroup, serverGroup.write());
        BenchmarkTiming res = Benchmark.stopTiming("server_group_add_template");
        CloudLogger.get().success("Successfully §aadded §b{} §rto the server group §b{}§r. §8(§rTook §b{}ms§8)", template.name(), serverGroup.name(), NumberUtils.formatNumber(res.duration(), 2));
        ServerGroupSyncPacket.create(serverGroup, false).broadcast();
    }

    public void removeTemplate(ServerGroup serverGroup, Template template) {
        Benchmark.startTiming("server_group_remove_template");
        ServerGroupRemoveTemplateEvent ev = new ServerGroupRemoveTemplateEvent(serverGroup, template);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("server_group_remove_template");
            return;
        }

        serverGroup.remove(template);
        CloudProvider.current().editServerGroup(serverGroup, serverGroup.write());
        BenchmarkTiming res = Benchmark.stopTiming("server_group_remove_template");
        CloudLogger.get().success("Successfully §cremoved §b{} §rfrom the server group §b{}§r. §8(§rTook §b{}ms§8)", template.name(), serverGroup.name(), NumberUtils.formatNumber(res.duration(), 2));
        ServerGroupSyncPacket.create(serverGroup, false).broadcast();
    }

    public void remove(ServerGroup serverGroup) {
        Benchmark.startTiming("server_group_removal");
        ServerGroupRemoveEvent ev = new ServerGroupRemoveEvent(serverGroup);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("server_group_removal");
            return;
        }

        CloudProvider.current().removeServerGroup(serverGroup);
        if (Files.isDirectory(serverGroup.path())) {
            try {
                FileUtils.removeDirectory(serverGroup.path());
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to remove server group directory of {}", e, serverGroup.name());
            }
        }

        serverGroups.remove(serverGroup.name());
        BenchmarkTiming res = Benchmark.stopTiming("server_group_removal");
        CloudLogger.get().success("Successfully §cremoved §rthe server group §b{}§r. §8(§rTook §b{}ms§8)", serverGroup.name(), NumberUtils.formatNumber(res.duration(), 2));
        ServerGroupSyncPacket.create(serverGroup, true).broadcast();
    }

    @Override
    public boolean check(String name) {
        return serverGroups.containsKey(name);
    }

    public Optional<ServerGroup> get(String name) {
        return Optional.ofNullable(serverGroups.getOrDefault(name, null));
    }

    @Override
    public Collection<ServerGroup> query(SearchQuery<? extends IServerGroup> searchQuery) {
        return filter(searchQuery);
    }

    @SuppressWarnings("unchecked")
    private <T extends IServerGroup> Collection<ServerGroup> filter(SearchQuery<T> query) {
        return serverGroups.values().stream()
                .filter(o -> query.matches((T) o))
                .toList();
    }

    public Collection<ServerGroup> getAll() {
        return serverGroups.values().stream().toList();
    }
}