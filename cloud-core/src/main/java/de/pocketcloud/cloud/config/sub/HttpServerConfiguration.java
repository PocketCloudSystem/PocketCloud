package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.common.util.StringUtils;
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
public final class HttpServerConfiguration extends OkaeriConfig implements ICloudConfig {

    @Comment({"Whether the http server will start"})
    private boolean enabled = false;

    @Comment({"Binding address for the cloud's HTTP server"})
    private String address = "127.0.0.1";

    @Comment({"Binding port for the cloud's HTTP server"})
    private int port = 8080;

    @CustomKey("auth-key")
    @Comment({"Authorization key for incoming HTTP requests"})
    private String authKey = StringUtils.generate(32);

    @Comment({"TLS/HTTPS configuration for the HTTP server"})
    private SslConfiguration ssl = new SslConfiguration();

    @Override
    public void validate() {
        if (port < 1 || port > 65535) {
            PocketCloud.instance().appendStartNotification("Invalid port given in http server configuration. reset to defaukt...", CloudLogLevel.WARN);
            port = 3656;
        }

        ssl.validate();
    }

    public SocketAddress socketAddress() {
        return new InetSocketAddress(address, port);
    }
}