package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.console.log.CloudLogger;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class ServerTimeoutsConfiguration extends OkaeriConfig implements ICloudConfig {

    @Comment({"Timeout for SERVER-type servers in seconds"})
    private int server = 15;

    @Comment({"Timeout for PROXY-type servers in seconds"})
    private int proxy = 15;

    @Override
    public void validate() {
        if (server <= 3) {
            CloudLogger.get().warn("Invalid server timeout given in config, reset to default...");
            server = 15;
        }

        if (proxy <= 3) {
            CloudLogger.get().warn("Invalid proxy timeout given in config, reset to default...");
            proxy = 15;
        }
    }

    public int timeout(TemplateType type) {
        return switch (type) {
            case PROXY -> proxy;
            case SERVER -> server;
        };
    }
}