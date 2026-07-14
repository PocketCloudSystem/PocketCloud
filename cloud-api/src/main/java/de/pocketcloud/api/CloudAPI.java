package de.pocketcloud.api;

import de.pocketcloud.api.language.ILanguage;
import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.model.player.ICloudPlayer;
import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.provider.*;
import de.pocketcloud.api.service.ServiceRegistry;

public interface CloudAPI {

    static CloudAPI instance() {
        return CloudAPIHolder.getInstance();
    }

    IModuleProvider modules();

    IPacketRegistry packets();

    IPlayerProvider<? extends ICloudPlayer> players();

    IServerGroupProvider<? extends IServerGroup> serverGroups();

    IServerProvider<? extends ICloudServer> servers();

    ITemplateProvider<? extends ITemplate> templates();

    ILanguageProvider<? extends ILanguage> language();

    ServiceRegistry services();

    default <T> T getService(Class<T> serviceClass) {
        return services().get(serviceClass);
    }
}