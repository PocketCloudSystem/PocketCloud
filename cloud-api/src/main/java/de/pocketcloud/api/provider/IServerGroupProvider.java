package de.pocketcloud.api.provider;

import de.pocketcloud.api.component.builder.IServerGroupBuilder;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.search.ServerGroupSearchQuery;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

public interface IServerGroupProvider {

    IServerGroupBuilder builder();

    boolean check(String name);

    Optional<IServerGroup> get(String name);

    Collection<IServerGroup> query(ServerGroupSearchQuery searchQuery);

    Collection<IServerGroup> query(Consumer<ServerGroupSearchQuery> queryConsumer);

    Collection<IServerGroup> getAll();
}