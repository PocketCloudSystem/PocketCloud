package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
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
public final class ClusterConfiguration extends OkaeriConfig implements ICloudConfig {

    @Exclude
    public static final List<String> STRATEGIES = List.of("least-memory", "fill", "round-robin");

    @Comment({"How the head picks the node for a new server (only used with more than one node).",
            "least-memory: the node with the most free memory budget (even distribution)",
            "fill: the node with the smallest free budget that is still enough (keeps machines free)",
            "round-robin: the next node in turn"})
    private String strategy = "least-memory";

    @Comment({"Where the nodes get the template files from.",
            "packets: from the head over the cluster connection (default, nothing to set up)",
            "s3: from an S3 bucket (AWS, MinIO, Cloudflare R2); the head uploads every new version once.",
            "Credentials: storage/internal/cluster/s3.properties (accessKey=..., secretKey=...)",
            "or the environment variables AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY."})
    private String templateStorage = "packets";

    @Comment({"S3 settings, only used with templateStorage: s3"})
    private S3Configuration s3 = new S3Configuration();

    @Override
    public void validate() {
        if (strategy == null || !STRATEGIES.contains(strategy.toLowerCase())) {
            CloudLogger.get().warn("Invalid cluster strategy §b{}§r, reset to default... §8(§bleast-memory§8)", strategy);
            strategy = "least-memory";
        }
        strategy = strategy.toLowerCase();
        if (templateStorage == null || !List.of("packets", "s3").contains(templateStorage.toLowerCase())) {
            CloudLogger.get().warn("Invalid cluster templateStorage §b{}§r, reset to default... §8(§bpackets§8)", templateStorage);
            templateStorage = "packets";
        }
        templateStorage = templateStorage.toLowerCase();
        s3.validate();
    }
}
