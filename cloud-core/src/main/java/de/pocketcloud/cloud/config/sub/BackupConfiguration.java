package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.Header;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
@Header({"Backups of templates, groups and configs (command: backup)."})
public final class BackupConfiguration extends OkaeriConfig implements ICloudConfig {

    @Comment({"Create a backup automatically every X hours (0 = off). In a cluster only the head does it."})
    private int intervalHours = 0;

    @Comment({"How many automatic backups are kept (older ones are deleted). Manual backups are never deleted."})
    private int keep = 7;

    @Comment({"Also upload every backup to the S3 bucket of s3 in cluster.yml (folder <prefix>backups/)."})
    private boolean uploadToS3 = false;

    @Override
    public void validate() {
        if (intervalHours < 0) intervalHours = 0;
        if (keep < 1) keep = 1;
    }
}
