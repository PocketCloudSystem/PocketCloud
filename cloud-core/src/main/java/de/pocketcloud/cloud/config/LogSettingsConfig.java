package de.pocketcloud.cloud.config;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.cloud.config.sub.DiscordWebhookConfiguration;
import de.pocketcloud.cloud.config.sub.PlayerLogsConfiguration;
import de.pocketcloud.cloud.console.log.CloudLogger;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class LogSettingsConfig extends OkaeriConfig implements ICloudConfig {

    @Comment({"Whether the debug mode should be enabled"})
    private boolean debugMode = false;

    @Comment({"The discord webhook configuration"})
    private DiscordWebhookConfiguration discordWebhook = new DiscordWebhookConfiguration();

    @Comment({"The notification configuration for console & in-game"})
    private PlayerLogsConfiguration playerLogs = new PlayerLogsConfiguration();

    @Override
    public void validate() {
        discordWebhook.validate();
        playerLogs.validate();
    }

    @Override
    public void apply() {
        CloudLogger.get().setDebugMode(debugMode);
    }
}