package de.pocketcloud.api.provider;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.common.concurrent.Promise;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface IServerProvider {

    default Promise<Collection<String>> start(ITemplate template) {
        return start(template, 1);
    }

    Promise<Collection<String>> start(ITemplate template, int count);

    Promise<Void> save(ICloudServer server);

    default Promise<Collection<ICloudServer>> stop(ICloudServer server) {
        return stop(server, false);
    }

    Promise<Collection<ICloudServer>> stop(ICloudServer server, boolean force);

    default Promise<Collection<ICloudServer>> stop(ITemplate template) {
        return stop(template, false);
    }

    Promise<Collection<ICloudServer>> stop(ITemplate template, boolean force);

    default Promise<Collection<ICloudServer>> stop(IServerGroup group) {
        return stop(group, false);
    }

    Promise<Collection<ICloudServer>> stop(IServerGroup group, boolean force);

    default Promise<Collection<ICloudServer>> stop(TemplateType type) {
        return stop(type, false);
    }

    Promise<Collection<ICloudServer>> stop(TemplateType type, boolean force);

    default Promise<Collection<ICloudServer>> stop(String name) {
        return stop(name, false);
    }

    Promise<Collection<ICloudServer>> stop(String name, boolean force);

    default Promise<Collection<ICloudServer>> stopAll() {
        return stopAll(false);
    }

    Promise<Collection<ICloudServer>> stopAll(boolean force);

    boolean check(String name);

    boolean check(UUID uuid);

    boolean checkCapacity(ITemplate template);

    Optional<ICloudServer> get(String name);

    Optional<ICloudServer> get(UUID uuid);

    /**
     * This method returns null on the cloud-side.
     */
    ICloudServer current();

    Collection<ICloudServer> query(ServerSearchQuery searchQuery);

    Collection<ICloudServer> query(Consumer<ServerSearchQuery> queryConsumer);

    int serverCount();

    Collection<ICloudServer> getAll();
}