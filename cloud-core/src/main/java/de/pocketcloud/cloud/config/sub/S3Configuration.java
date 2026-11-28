package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.cloud.console.log.CloudLogger;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class S3Configuration extends OkaeriConfig implements ICloudConfig {

    @Comment({"URL of the S3 service, e.g. https://s3.eu-central-1.amazonaws.com, http://127.0.0.1:9000 (MinIO)",
            "or https://<account>.r2.cloudflarestorage.com"})
    private String endpoint = "https://s3.eu-central-1.amazonaws.com";

    @Comment({"Region of the bucket (MinIO: us-east-1, Cloudflare R2: auto)."})
    private String region = "eu-central-1";

    @Comment({"Bucket for the template files. Use the same bucket and prefix on all nodes."})
    private String bucket = "pocketcloud";

    @Comment({"Folder inside the bucket. Letters, digits, '-', '_', '.' and '/'."})
    private String prefix = "templates/";

    @Override
    public void validate() {
        endpoint = endpoint == null ? "" : endpoint.trim();
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            CloudLogger.get().warn("Invalid S3 endpoint §b{}§r, it must start with http:// or https://.", endpoint);
        }
        if (prefix == null || !prefix.matches("[A-Za-z0-9._/-]*")) {
            CloudLogger.get().warn("Invalid S3 prefix §b{}§r, reset to default... §8(§btemplates/§8)", prefix);
            prefix = "templates/";
        }
        if (bucket == null || !bucket.matches("[a-z0-9][a-z0-9.-]{1,61}[a-z0-9]")) {
            CloudLogger.get().warn("Invalid S3 bucket name §b{}§r (3-63 characters: a-z, 0-9, '.', '-').", bucket);
        }
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix += "/";
        if (prefix.startsWith("/")) prefix = prefix.substring(1);
    }
}
