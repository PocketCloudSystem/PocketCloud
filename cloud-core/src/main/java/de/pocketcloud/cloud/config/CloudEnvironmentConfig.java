package de.pocketcloud.cloud.config;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.config.IEnvironmentConfig;
import de.pocketcloud.cloud.PocketCloud;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.UUID;

public final class CloudEnvironmentConfig implements IEnvironmentConfig {

    @Override
    public SocketAddress cloudNetworkAddress() {
        return PocketCloud.instance().network().address();
    }

    @Override
    public SocketAddress localNetworkAddress() {
        return cloudNetworkAddress();
    }

    @Override
    public String networkAuthKey() {
        return PocketCloud.instance().network().authToken();
    }

    @Override
    public boolean networkEncryption() {
        return PocketCloud.instance().network().encryption();
    }

    @Override
    public int networkPacketSizeLimit() {
        return PocketCloud.instance().network().packetSizeLimit();
    }

    @Override
    public String localServerName() {
        return "pocketcloud";
    }

    @Override
    public UUID localServerUuid() {
        return null;
    }

    @Override
    public String localTemplateName() {
        return null;
    }

    @Override
    public int localServerTimeout() {
        return 0;
    }

    @Override
    public Path cloudPath() {
        return Path.of(System.getProperty("user.dir"));
    }

    @Override
    public String language() {
        return CloudAPI.instance().language().current().id();
    }
}