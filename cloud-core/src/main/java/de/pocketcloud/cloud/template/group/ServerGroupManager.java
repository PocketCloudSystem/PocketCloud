package de.pocketcloud.cloud.template.group;

import de.pocketcloud.api.component.builder.IServerGroupBuilder;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.provider.write.IWriteServerGroupProvider;
import de.pocketcloud.api.search.ServerGroupSearchQuery;
import de.pocketcloud.cloud.builder.ServerGroupBuilder;
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
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;

public final class ServerGroupManager implements Loadable, IWriteServerGroupProvider {

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

    @Override
    public void create(IServerGroupBuilder builder) {
        ServerGroup serverGroup = (ServerGroup) builder.build();
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
            add(serverGroup);
            BenchmarkTiming res = Benchmark.stopTiming("server_group_creation");
            CloudLogger.get().success("Successfully §acreated §rthe server group §b{}§r. §8(§rTook §b{}ms§8)", serverGroup.name(), NumberUtils.formatNumber(res.duration(), 2));
            serverGroup.syncOut();
        } catch (IOException e) {
            CloudLogger.get().exception("Failed to create server group {}", e, serverGroup.name());
            Benchmark.stopTiming("server_group_creation");
            serverGroups.remove(serverGroup.name());
        }
    }

    @Override
    public void add(IServerGroup serverGroup) {
        serverGroups.put(serverGroup.name(), requireServerGroup(serverGroup));
    }

    @Override
    public void addTemplate(IServerGroup group, ITemplate template) {
        ServerGroup serverGroup = requireServerGroup(group);
        Benchmark.startTiming("server_group_add_template");
        ServerGroupAddTemplateEvent ev = new ServerGroupAddTemplateEvent(serverGroup, (Template) template);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("server_group_add_template");
            return;
        }

        Collection<String> templates = new ArrayList<>(serverGroup.templates());
        templates.add(template.name());
        serverGroup.templates(templates);

        CloudProvider.current().editServerGroup(serverGroup, serverGroup.write());
        BenchmarkTiming res = Benchmark.stopTiming("server_group_add_template");
        CloudLogger.get().success("Successfully §aadded §b{} §rto the server group §b{}§r. §8(§rTook §b{}ms§8)", template.name(), serverGroup.name(), NumberUtils.formatNumber(res.duration(), 2));
        serverGroup.syncOut();
    }

    @Override
    public void removeTemplate(IServerGroup group, ITemplate template) {
        ServerGroup serverGroup = requireServerGroup(group);
        Benchmark.startTiming("server_group_remove_template");
        ServerGroupRemoveTemplateEvent ev = new ServerGroupRemoveTemplateEvent(serverGroup, (Template) template);
        ev.call();
        if (ev.isCancelled()) {
            Benchmark.stopTiming("server_group_remove_template");
            return;
        }

        Collection<String> templates = new ArrayList<>(serverGroup.templates());
        templates.remove(template.name());
        serverGroup.templates(templates);

        CloudProvider.current().editServerGroup(serverGroup, serverGroup.write());
        BenchmarkTiming res = Benchmark.stopTiming("server_group_remove_template");
        CloudLogger.get().success("Successfully §cremoved §b{} §rfrom the server group §b{}§r. §8(§rTook §b{}ms§8)", template.name(), serverGroup.name(), NumberUtils.formatNumber(res.duration(), 2));
        serverGroup.syncOut();
    }

    @Override
    public void remove(IServerGroup serverGroup) {
        serverGroups.remove(serverGroup.name());
    }

    @Override
    public void delete(IServerGroup group) {
        ServerGroup serverGroup = requireServerGroup(group);
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

        remove(serverGroup);
        BenchmarkTiming res = Benchmark.stopTiming("server_group_removal");
        CloudLogger.get().success("Successfully §cremoved §rthe server group §b{}§r. §8(§rTook §b{}ms§8)", serverGroup.name(), NumberUtils.formatNumber(res.duration(), 2));
        serverGroup.markForRemoval().syncOut();
    }

    @Override
    public IServerGroupBuilder builder() {
        return new ServerGroupBuilder();
    }

    @Override
    public boolean check(String name) {
        return serverGroups.containsKey(name);
    }

    public Optional<IServerGroup> get(String name) {
        return Optional.ofNullable(serverGroups.get(name));
    }

    @Override
    public Collection<IServerGroup> query(ServerGroupSearchQuery searchQuery) {
        return widen(serverGroups.values().stream()
                .filter(searchQuery::matches)
                .toList());
    }

    @Override
    public Collection<IServerGroup> query(Consumer<ServerGroupSearchQuery> queryConsumer) {
        ServerGroupSearchQuery searchQuery = new ServerGroupSearchQuery();
        queryConsumer.accept(searchQuery);
        return query(searchQuery);
    }

    public Collection<IServerGroup> getAll() {
        return widen(serverGroups.values().stream().toList());
    }

    @SuppressWarnings("unchecked")
    private <T extends IServerGroup> Collection<IServerGroup> widen(Collection<T> collection) {
        return (Collection<IServerGroup>) collection;
    }

    private ServerGroup requireServerGroup(IServerGroup group) {
        if (!(group instanceof ServerGroup serverGroup)) {
            throw new IllegalArgumentException("Unsupported ICloudServer implementation: " + group.getClass().getName());
        }

        return serverGroup;
    }
}