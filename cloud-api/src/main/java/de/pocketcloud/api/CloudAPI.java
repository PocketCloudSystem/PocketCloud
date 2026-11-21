package de.pocketcloud.api;

import de.pocketcloud.api.cluster.INodeProvider;

import de.pocketcloud.api.config.IEnvironmentConfig;
import de.pocketcloud.api.event.EventService;
import de.pocketcloud.api.executor.IPlayerExecutor;
import de.pocketcloud.api.logging.ILogger;
import de.pocketcloud.api.provider.*;
import de.pocketcloud.api.service.ServiceRegistry;

public interface CloudAPI {

    static CloudAPI instance() {
        return CloudAPIHolder.getInstance();
    }

    IPlayerExecutor playerExecutor();

    IPacketRegistry<?> packets();

    IPlayerProvider players();

    IServerGroupProvider serverGroups();

    IServerProvider servers();

    ITemplateProvider templates();

    ISoftwareProvider softwares();

    ILanguageProvider language();

    EventService<?> events();

    ILogger logger();

    IEnvironmentConfig environmentConfig();

    ServiceRegistry services();

    /**
     * The nodes of the cloud. Without a cluster there is exactly one node.
     */
    default INodeProvider nodes() {
        Object provider = services().getAll().get(INodeProvider.class);
        return provider instanceof INodeProvider nodes ? nodes : INodeProvider.EMPTY;
    }

    default <T> T service(Class<T> serviceClass) {
        return services().get(serviceClass);
    }
}