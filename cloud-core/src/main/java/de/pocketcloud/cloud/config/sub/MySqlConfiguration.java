package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.provider.database.MySqlSettings;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Getter
@Setter
@Accessors(fluent = true)
public final class MySqlConfiguration extends OkaeriConfig implements ICloudConfig {

    @Comment({"MySQL server address"})
    private String address = "127.0.0.1";

    @Comment({"MySQL server port"})
    private int port = 3306;

    @Comment({"Used MySQL database"})
    private String database = "cloud";

    @Comment({"Used MySQL username"})
    private String user = "root";

    @Comment({"Used MySQL user password"})
    private String password = "123";

    @Override
    public void validate() {
        if (port < 1 || port > 65535) {
            PocketCloud.instance().appendStartNotification("Invalid port given in network configuration. reset to defaukt...", CloudLogLevel.WARN);
            port = 3656;
        }
    }

    public MySqlSettings asSettings() {
        return new MySqlSettings(address, port, user, database, password);
    }

    public SocketAddress socketAddress() {
        return new InetSocketAddress(address, port);
    }
}