package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.builder.IServerGroupBuilder;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.provider.write.IWriteServerGroupProvider;
import de.pocketcloud.api.search.ServerGroupSearchQuery;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.component.ServerGroup;
import de.pocketcloud.bridge.component.builder.ServerGroupBuilder;
import de.pocketcloud.shared.event.group.ServerGroupCreatedEvent;
import de.pocketcloud.shared.event.group.ServerGroupDeletedEvent;
import de.pocketcloud.shared.event.group.ServerGroupUpdatedEvent;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ServerGroupProvider implements IWriteServerGroupProvider {

    private final Map<String, IServerGroup> serverGroups = new ConcurrentHashMap<>();

    @Override
    public IServerGroupBuilder builder() {
        return new ServerGroupBuilder();
    }

    @Override
    public void create(IServerGroupBuilder builder) {
        throw new UnsupportedOperationException("You cannot create server groups on cloud servers");
    }

    @Override
    public void delete(IServerGroup serverGroup) {
        throw new UnsupportedOperationException("You cannot delete server groups on cloud servers");
    }

    @Override
    public void add(IServerGroup serverGroup) {
        boolean verified = CloudBridge.instance().status().isVerified();
        if (serverGroups.containsKey(serverGroup.name())) {
            ServerGroup localServerGroup = (ServerGroup) serverGroups.get(serverGroup.name());
            localServerGroup.syncIn(serverGroup);
            if (verified) CloudAPI.instance().events().call(new ServerGroupUpdatedEvent(localServerGroup, localServerGroup.templates(), serverGroup.templates()));
        } else {
            if (verified) CloudAPI.instance().events().call(new ServerGroupCreatedEvent(serverGroup));
            serverGroups.put(serverGroup.name(), serverGroup);
        }
    }

    @Override
    public void addTemplate(IServerGroup serverGroup, ITemplate template) {
        throw new UnsupportedOperationException("You cannot add a template to a server group on cloud servers");
    }

    @Override
    public void removeTemplate(IServerGroup serverGroup, ITemplate template) {
        throw new UnsupportedOperationException("You cannot remove a template from a server group on cloud servers");
    }

    @Override
    public void remove(IServerGroup serverGroup) {
        ServerGroup localServerGroup = (ServerGroup) serverGroups.get(serverGroup.name());
        if (CloudBridge.instance().status().isVerified()) CloudAPI.instance().events().call(new ServerGroupDeletedEvent(localServerGroup));
        serverGroups.remove(serverGroup.name());
    }

    @Override
    public boolean check(String name) {
        return serverGroups.containsKey(name);
    }

    @Override
    public Optional<IServerGroup> get(String name) {
        return Optional.ofNullable(serverGroups.get(name));
    }

    @Override
    public Collection<IServerGroup> query(ServerGroupSearchQuery searchQuery) {
        return serverGroups.values().stream()
                .filter(searchQuery::matches)
                .toList();
    }

    @Override
    public Collection<IServerGroup> query(Consumer<ServerGroupSearchQuery> queryConsumer) {
        ServerGroupSearchQuery searchQuery = new ServerGroupSearchQuery();
        queryConsumer.accept(searchQuery);
        return query(searchQuery);
    }

    @Override
    public Collection<IServerGroup> getAll() {
        return serverGroups.values().stream().toList();
    }
}