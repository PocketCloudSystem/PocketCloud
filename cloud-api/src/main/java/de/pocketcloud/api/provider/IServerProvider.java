package de.pocketcloud.api.provider;

import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.search.SearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.common.concurrent.Promise;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IServerProvider<T extends ICloudServer> {

    void add(T server);

    void remove(T server);

    default Promise<Collection<String>> start(ITemplate template) {
        return start(template, 1);
    }

    Promise<Collection<String>> start(ITemplate template, int count);

    Promise<Void> save(T server);

    default Promise<Collection<T>> stop(T server) {
        return stop(server, false);
    }

    Promise<Collection<T>> stop(T server, boolean force);

    default Promise<Collection<T>> stop(ITemplate template) {
        return stop(template, false);
    }

    Promise<Collection<T>> stop(ITemplate template, boolean force);

    default Promise<Collection<T>> stop(IServerGroup group) {
        return stop(group, false);
    }

    Promise<Collection<T>> stop(IServerGroup group, boolean force);

    default Promise<Collection<T>> stop(TemplateType type) {
        return stop(type, false);
    }

    Promise<Collection<T>> stop(TemplateType type, boolean force);

    default Promise<Collection<T>> stop(String name) {
        return stop(name, false);
    }

    Promise<Collection<T>> stop(String name, boolean force);

    default Promise<Collection<T>> stopAll() {
        return stopAll(false);
    }

    Promise<Collection<T>> stopAll(boolean force);

    boolean check(String name);

    boolean check(UUID uuid);

    boolean checkCapacity(ITemplate template);

    Optional<T> get(String name);

    Optional<T> get(UUID uuid);

    Collection<T> query(SearchQuery<? extends ICloudServer> searchQuery);

    Collection<T> getAll();
}