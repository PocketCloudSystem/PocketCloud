package de.pocketcloud.cloud.config;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.cloud.config.sub.HttpServerConfiguration;
import de.pocketcloud.cloud.config.sub.MySqlConfiguration;
import de.pocketcloud.cloud.config.sub.NetworkConfiguration;
import de.pocketcloud.cloud.console.log.CloudLogger;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Exclude;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Setter
@Accessors(fluent = true)
public final class MainConfig extends OkaeriConfig implements ICloudConfig {

    @Exclude
    public static List<String> AVAILABLE_PROVIDERS = List.of("json", "mysql");

    @Comment({"The name of the cloud."})
    private String cloudName = "main-cloud";

    @Comment({"The language used in-game."})
    private String language = "en_US";

    @Comment({"The data provider used for storing data.", "Available providers are: json, mysql"})
    private String provider = "json";

    @Comment({"Whether the recorded timings should be written into a file on cloud shutdown."})
    private boolean writeTimingsOnShutdown = true;

    @Comment({"The network configuration for the cloud."})
    private NetworkConfiguration network = new NetworkConfiguration();

    @Comment({"The HTTP service configuration for the cloud."})
    private HttpServerConfiguration httpServer = new HttpServerConfiguration();

    @Comment({"The MySQL configuration for the cloud. Only used if provider is set to 'mysql'"})
    private MySqlConfiguration mysqlSettings = new MySqlConfiguration();

    @Override
    public void validate() {
        if (!AVAILABLE_PROVIDERS.contains(provider.toLowerCase())) {
            provider = "json";
            CloudLogger.get().warn("Invalid provider given in config, reset to default... §8(§bjson§8)");
        }

        network.validate();
        httpServer.validate();
        mysqlSettings.validate();
    }
}