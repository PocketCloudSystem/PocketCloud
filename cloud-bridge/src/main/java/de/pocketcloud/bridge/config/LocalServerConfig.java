package de.pocketcloud.bridge.config;

import de.pocketcloud.api.config.IEnvironmentConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public final class LocalServerConfig implements IEnvironmentConfig {

    private final SocketAddress cloudNetworkAddress;
    private final String networkAuthKey;
    private final boolean networkEncryption;
    private final int networkPacketSizeLimit;
    private final String localServerName;
    private final UUID localServerUuid;
    private final String localTemplateName;
    private final int localServerTimeout;
    private final Path cloudPath;
    private final String language;

    @Setter
    private SocketAddress localNetworkAddress = null;

    public LocalServerConfig(SocketAddress cloudNetworkAddress, String networkAuthKey, boolean networkEncryption, int networkPacketSizeLimit, String localServerName, UUID localServerUuid, String localTemplateName, int localServerTimeout, Path cloudPath, String language) {
        this.cloudNetworkAddress = cloudNetworkAddress;
        this.networkAuthKey = networkAuthKey;
        this.networkEncryption = networkEncryption;
        this.networkPacketSizeLimit = networkPacketSizeLimit;
        this.localServerName = localServerName;
        this.localServerUuid = localServerUuid;
        this.localTemplateName = localTemplateName;
        this.localServerTimeout = localServerTimeout;
        this.cloudPath = cloudPath;
        this.language = language;
    }
}