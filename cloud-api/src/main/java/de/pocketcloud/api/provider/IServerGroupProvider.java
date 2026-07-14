package de.pocketcloud.api.provider;

import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.search.SearchQuery;

import java.util.Collection;
import java.util.Optional;

public interface IServerGroupProvider<T extends IServerGroup> {

    void add(T serverGroup);

    void remove(T serverGroup);

    boolean check(String name);

    Optional<T> get(String name);

    Collection<T> query(SearchQuery<? extends IServerGroup> searchQuery);

    Collection<T> getAll();
}