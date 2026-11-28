package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.cloud.PocketCloud;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
@Header({"The Redis configuration for the cloud. Holds the shared cluster state and caches the provider data."})
public final class RedisConfiguration extends OkaeriConfig implements ICloudConfig {

    @Comment({"Whether Redis is used as cache in front of the provider and as optional shared state of a cluster",
            "(head fencing, server snapshot for faster recovery). Optional: a cluster works without Redis."})
    private boolean enabled = false;

    @Comment({"Redis server address"})
    private String address = "127.0.0.1";

    @Comment({"Redis server port"})
    private int port = 6379;

    @Comment({"Redis username (leave empty when ACLs are not used)"})
    private String username = "";

    @Comment({"Redis password (leave empty when no password is set)"})
    private String password = "";

    @Comment({"Redis database index"})
    private int database = 0;

    @Comment({"Whether the connection to Redis uses TLS"})
    private boolean ssl = false;

    @Comment({"Timeout for Redis commands in milliseconds"})
    private int timeout = 3000;

    @Override
    public void validate() {
        if (port < 1 || port > 65535) {
            PocketCloud.instance().appendStartNotification("Invalid port given in redis configuration, reset to default...", CloudLogLevel.WARN);
            port = 6379;
        }

        if (database < 0) database = 0;
        if (timeout < 100) timeout = 3000;
    }
}
