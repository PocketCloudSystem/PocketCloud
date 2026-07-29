package de.pocketcloud.bridge.config;

import de.pocketcloud.api.config.IEnvironmentConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Map;
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

    public static LocalServerConfig fromMap(Map<String, Object> map) {
        return new LocalServerConfig(
                new InetSocketAddress(map.get("network-address").toString(), Integer.parseInt(map.get("network-port").toString())),
                map.get("network-auth-key").toString(),
                Boolean.parseBoolean(map.get("network-encryption").toString()),
                Integer.parseInt(map.get("network-packet-size-limit").toString()),
                map.get("server-name").toString(),
                UUID.fromString(map.get("server-uuid").toString()),
                map.get("template-name").toString(),
                Integer.parseInt(map.get("server-timeout").toString()),
                Path.of(map.get("cloud-path").toString()),
                map.get("cloud-language").toString()
        );
    }
}