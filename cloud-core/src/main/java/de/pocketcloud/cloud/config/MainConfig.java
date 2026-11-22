package de.pocketcloud.cloud.config;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.cloud.config.sub.*;
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

    @Comment({"Whether the cloud should check for updates on startup.", "Only applies to cloud updates."})
    private boolean checkForUpdates = true;

    @Comment({"Whether the cloud should check your server properties/config for updates on startup."})
    private boolean checkProperties = true;

    @Comment({"Custom locations of storage folders. Relative paths start in the cloud folder, absolute paths can point anywhere.", "Changes need a restart."})
    private PathsConfiguration paths = new PathsConfiguration();

    @Comment({"The network configuration for the cloud."})
    private NetworkConfiguration network = new NetworkConfiguration();

    @Comment({"The HTTP service configuration for the cloud."})
    private HttpServerConfiguration httpServer = new HttpServerConfiguration();

    @Comment({"The MySQL configuration for the cloud. Only used if provider is set to 'mysql'"})
    private MySqlConfiguration mysqlSettings = new MySqlConfiguration();

    @Comment({"The Redis configuration for the cloud. Holds the shared cluster state and caches the provider data."})
    private RedisConfiguration redis = new RedisConfiguration();

    @Comment({"Settings for running several nodes as one cluster."})
    private ClusterConfiguration cluster = new ClusterConfiguration();

    @Override
    public void validate() {
        if (!AVAILABLE_PROVIDERS.contains(provider.toLowerCase())) {
            provider = "json";
            CloudLogger.get().warn("Invalid provider given in config, reset to default... §8(§bjson§8)");
        }

        paths.validate();
        network.validate();
        httpServer.validate();
        mysqlSettings.validate();
        redis.validate();
        cluster.validate();
    }
}