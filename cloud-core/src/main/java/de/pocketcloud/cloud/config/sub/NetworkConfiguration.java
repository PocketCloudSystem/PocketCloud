package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.cloud.PocketCloud;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Getter
@Setter
@Accessors(fluent = true)
public final class NetworkConfiguration extends OkaeriConfig implements ICloudConfig {

    @Comment({"Binding address for the cloud's socket"})
    private String address = "127.0.0.1";

    @Comment({"Binding port for the cloud's socket"})
    private int port = 3656;

    @Comment({"Whether the traffic should be encrypted", "NOTE: less performant"})
    private boolean encryption = false;

    @CustomKey("packet-size-limit")
    @Comment({"Highest possible packet size in bytes.", "Default is 1MB"})
    private int packetSizeLimit = 1_048_576;

    @Override
    public void validate() {
        if (port < 1 || port > 65535) {
            PocketCloud.instance().appendStartNotification("Invalid port given in network configuration. reset to defaukt...", CloudLogLevel.WARN);
            port = 3656;
        }

        if (packetSizeLimit < 1024) {
            PocketCloud.instance().appendStartNotification("Invalid packet size limit given in network configuration. reset to defaukt...", CloudLogLevel.WARN);
            packetSizeLimit = 1_048_576;
        }
    }

    public SocketAddress socketAddress() {
        return new InetSocketAddress(address, port);
    }
}