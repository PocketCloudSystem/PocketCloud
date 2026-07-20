package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.component.builder.IServerGroupBuilder;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.provider.write.IWriteServerGroupProvider;
import de.pocketcloud.api.search.ServerGroupSearchQuery;
import de.pocketcloud.bridge.component.ServerGroup;
import de.pocketcloud.bridge.component.builder.ServerGroupBuilder;
import org.apache.commons.lang3.NotImplementedException;

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
        throw new NotImplementedException("You cannot create server groups on cloud servers");
    }

    @Override
    public void delete(IServerGroup serverGroup) {
        throw new NotImplementedException("You cannot delete server groups on cloud servers");
    }

    @Override
    public void add(IServerGroup serverGroup) {
        if (serverGroups.containsKey(serverGroup.name())) {
            ((ServerGroup) serverGroups.get(serverGroup.name())).syncIn(serverGroup);
        } else serverGroups.put(serverGroup.name(), serverGroup);
    }

    @Override
    public void addTemplate(IServerGroup serverGroup, ITemplate template) {
        throw new NotImplementedException("You cannot add a template to a server group on cloud servers");
    }

    @Override
    public void removeTemplate(IServerGroup serverGroup, ITemplate template) {
        throw new NotImplementedException("You cannot remove a template from a server group on cloud servers");
    }

    @Override
    public void remove(IServerGroup serverGroup) {
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