package de.pocketcloud.cloud.config;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.cloud.config.sub.ServerAddressesConfiguration;
import de.pocketcloud.cloud.config.sub.ServerPortRangesConfiguration;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.util.ServerStartMethods;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class ServerSettingsConfig extends OkaeriConfig implements ICloudConfig {

    @Comment({"Server start method, used to boot a server.", "Available: proc, screen, tmux"})
    private String startMethod = "proc";

    @Comment({"Server port ranges for the template types."})
    private ServerPortRangesConfiguration serverPortRanges = new ServerPortRangesConfiguration();

    @Comment({"Addresses used by the servers and proxies started by the cloud."})
    private ServerAddressesConfiguration addresses = new ServerAddressesConfiguration();

    @Override
    public void validate() {
        if (ServerStartMethods.get(startMethod.toLowerCase()).isEmpty()) {
            CloudLogger.get().warn("Invalid start method given in config, reset to default... §8(§bproc§8)");
            startMethod = "proc";
        }

        addresses.validate();
    }

    @Override
    public void apply() {
        ServerStartMethods.set(ServerStartMethods.get(startMethod.toLowerCase()).get());
    }
}