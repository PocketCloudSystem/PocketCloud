package de.pocketcloud.cloud.config.sub;

import de.pocketcloud.api.config.ICloudConfig;
import de.pocketcloud.api.logging.CloudLogLevel;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.util.NetUtils;
import de.pocketcloud.shared.network.packet.type.NotificationType;
import de.r3pt1s.discord.webhook.Webhook;
import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import eu.okaeri.configs.annotation.CustomKey;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public final class DiscordWebhookConfiguration extends OkaeriConfig implements ICloudConfig {

    private boolean enabled = false;

    @CustomKey("webhook-url")
    private String webhookUrl = "your-webhook-url";

    private DiscordUserConfiguration user = new DiscordUserConfiguration();

    private NotificationsConfiguration notifications = new NotificationsConfiguration();

    @Override
    public void validate() {
        if (enabled && !NetUtils.isValidUrl(webhookUrl)) {
            PocketCloud.instance().appendStartNotification("Invalid webhook-url given in discord webhook configuration, disabling discord webhook...", CloudLogLevel.WARN);
            enabled = false;
        }
    }

    public Webhook buildWebhook() {
        if (!enabled) return null;
        return new Webhook(webhookUrl).withDefaults(user.username.replace("%cloud_name%", PocketCloud.instance().config().cloudName()), user.avatarUrl);
    }

    public boolean canSendWebhook(NotificationType type) {
        if (!enabled) return false;

        return switch (type) {
            case SERVER_CRASHED -> notifications.crashedServers;
            case SERVER_TIMED_OUT, SERVER_STOP_TIMED_OUT -> notifications.timedOutServers;
            case SERVER_START_FAILED -> notifications.failedServerStarts;
            case PLAYER_JOIN_FAILED -> notifications.failedPlayerJoins;
            case PLAYER_KICKED -> notifications.playerKicks;
            default -> false;
        };
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class DiscordUserConfiguration extends OkaeriConfig implements ICloudConfig {

        @Comment({"Username of the discord webhook sending the messages"})
        private String username = "PocketCloud Notifications | #%cloud_name%";

        @CustomKey("avatar-url")
        @Comment({"Avatar URL of the discord webhook sending the messages"})
        private String avatarUrl = "https://avatars.githubusercontent.com/u/97796660?s=200&v=4";

        @Override
        public void validate() {
            if (!NetUtils.isValidUrl(avatarUrl)) {
                CloudLogger.get().warn("Invalid avatar-url given in discord webhook configuration, reset to default...");
                avatarUrl = "https://avatars.githubusercontent.com/u/97796660";
            }
        }
    }

    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class NotificationsConfiguration extends OkaeriConfig implements ICloudConfig {

        @CustomKey("crashed-servers")
        private boolean crashedServers = false;

        @CustomKey("timed-out-servers")
        private boolean timedOutServers = false;

        @CustomKey("failed-server-starts")
        private boolean failedServerStarts = false;

        @CustomKey("failed-player-joins")
        private boolean failedPlayerJoins = false;

        @CustomKey("player-kicks")
        private boolean playerKicks = false;

        @Override
        public void validate() {}
    }
}