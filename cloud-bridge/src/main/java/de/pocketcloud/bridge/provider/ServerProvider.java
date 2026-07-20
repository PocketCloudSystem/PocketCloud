package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.provider.write.IWriteServerProvider;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.component.CloudServer;
import de.pocketcloud.common.concurrent.Promise;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ServerProvider implements IWriteServerProvider {

    private final Map<String, ICloudServer> servers = new ConcurrentHashMap<>();

    @Override
    public void add(ICloudServer server) {
        if (servers.containsKey(server.name())) {
            ((CloudServer) servers.get(server.name())).syncIn(server);
        } else servers.put(server.name(), server);
    }

    @Override
    public void remove(ICloudServer server) {
        servers.remove(server.name());
    }

    @Override
    public Promise<Collection<String>> start(ITemplate template, int count) {
        return null; //todo request packet
    }

    @Override
    public Promise<Void> save(ICloudServer server) {
        return null; // todo request packet
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(ICloudServer server, boolean force) {
        return null; // todo request packet
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(ITemplate template, boolean force) {
        return null; // todo request packet
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(IServerGroup group, boolean force) {
        return null; // todo request packet
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(TemplateType type, boolean force) {
        return null; // todo request packet
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(String name, boolean force) {
        return null; // todo request packet
    }

    @Override
    public Promise<Collection<ICloudServer>> stopAll(boolean force) {
        return null; // todo request packet
    }

    @Override
    public boolean check(String name) {
        return servers.containsKey(name);
    }

    @Override
    public boolean check(UUID uuid) {
        return servers.values().stream().anyMatch(s -> s.uuid().equals(uuid));
    }

    @Override
    public boolean checkCapacity(ITemplate template) {
        return query(ServerSearchQuery.create().ofTemplate(template)).size() < template.settings().maxServerCount();
    }

    @Override
    public Optional<ICloudServer> get(String name) {
        return Optional.ofNullable(servers.get(name));
    }

    @Override
    public Optional<ICloudServer> get(UUID uuid) {
        return servers.values().stream().filter(s -> s.uuid().equals(uuid)).findFirst();
    }

    @Override
    public ICloudServer current() {
        return Optional.ofNullable(servers.get(CloudBridge.instance().environmentConfig().localServerName())).orElseThrow(() -> new IllegalStateException("Current server should not be null, called too early?"));
    }

    @Override
    public Collection<ICloudServer> query(ServerSearchQuery searchQuery) {
        return servers.values().stream()
                .filter(searchQuery::matches)
                .toList();
    }

    @Override
    public Collection<ICloudServer> query(Consumer<ServerSearchQuery> queryConsumer) {
        ServerSearchQuery searchQuery = ServerSearchQuery.create();
        queryConsumer.accept(searchQuery);
        return servers.values().stream()
                .filter(searchQuery::matches)
                .toList();
    }

    @Override
    public Collection<ICloudServer> getAll() {
        return servers.values().stream().toList();
    }
}