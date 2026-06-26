package de.pocketcloud.cloud.config.impl;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogLevel;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.packet.type.NotificationType;
import de.pocketcloud.cloud.util.net.NetUtils;
import de.pocketcloud.configlib.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Setter
public final class LogSettingsConfig extends Configuration {

    @Ignored
    public static final String CATEGORY_CONNECTION = "connection_lifecycle";
    @Ignored
    public static final String CATEGORY_FAILED_JOINS = "failed_joins";
    @Ignored
    public static final String CATEGORY_KICKS = "kicks";
    @Ignored
    public static final String CATEGORY_SERVER_SWITCHED = "server_switched";

    @Ignored
    @Getter
    @Accessors(fluent = true)
    private static LogSettingsConfig instance = null;

    @Comment({"Whether the debug mode should be enabled"})
    private boolean debugMode = false;

    @Comment({"The discord webhook configuration"})
    private ConfigMap discordWebhook = new ConfigMap()
            .set("enabled", false)
            .set("webhook-url", "your-discord-webhook-url")
            .set("notifications", new ConfigMap()
                    .set("crashed_servers", true)
                    .set("timed_out_servers", true)
                    .set("failed_server_starts", true)
                    .set("failed_player_joins", true)
                    .set("player_kicks", true)
            );

    @Comment({"The notification configuration for console & in-game"})
    private ConfigMap playerLogs = new ConfigMap()
            .set(CATEGORY_CONNECTION, new ConfigMap()
                            .set("console", true)
                            .set("in_game", true),
                    "Regular player join/leave messages")
            .set(CATEGORY_FAILED_JOINS, new ConfigMap()
                            .set("console", true)
                            .set("in_game", true),
                    "Fires when a player gets kicked during login sequence")
            .set(CATEGORY_KICKS, new ConfigMap()
                            .set("console", true)
                            .set("in_game", true),
                    "Regular kick via ingame or cloud messages")
            .set(CATEGORY_SERVER_SWITCHED, new ConfigMap()
                            .set("console", true)
                            .set("in_game", true),
                    "Regular player server switching messages");

    public LogSettingsConfig() {
        super("storage/configs/log_settings.yml", ConfigType.YAML);
        instance = this;
        reload();
    }

    public void reload() {
        var changes = new AtomicInteger(0);
        boolean loadFailed = !load(changes);

        String webhookUrl = discordWebhook.get("webhook-url").toString();
        boolean webhookEnabled = Boolean.parseBoolean(discordWebhook.get("enabled").toString());

        if (webhookEnabled && !NetUtils.isValidUrl(webhookUrl)) {
            PocketCloud.instance().appendStartNotification(
                    "Invalid webhook url inside §blog_settings.yml§r. Resetting to default value...",
                    CloudLogLevel.WARN
            );
            discordWebhook.set("webhook-url", "your-discord-webhook-url");
            changes.incrementAndGet();
        }

        CloudLogger.get().setDebugMode(debugMode);

        if (loadFailed || changes.get() > 0) save();
    }

    private void assertCategory(String category) {
        if (!playerLogs.toRawMap().containsKey(category)) {
            throw new IllegalArgumentException("Unknown player log category: " + category);
        }
    }

    public String getWebhookUrl() {
        return discordWebhook.toRawMap().getOrDefault("webhook-url", "").toString();
    }

    public boolean isWebhookEnabled() {
        return Boolean.parseBoolean(discordWebhook.get("enabled").toString()) && NetUtils.isValidUrl(getWebhookUrl());
    }

    public LogSettingsConfig setDiscordWebhookEnabled(boolean enabled) {
        discordWebhook.set("enabled", enabled);
        return this;
    }

    public LogSettingsConfig setDiscordWebhookUrl(String url) {
        if (url == null) {
            discordWebhook.set("webhook-url", "your-discord-webhook-url");
        } else if (NetUtils.isValidUrl(url)) {
            discordWebhook.set("webhook-url", url);
        } else {
            throw new IllegalArgumentException("Invalid webhook url: " + url);
        }
        return this;
    }

    public LogSettingsConfig setDiscordWebhookNotification(String type, boolean enabled) {
        ConfigMap notifications = (ConfigMap) discordWebhook.get("notifications");
        notifications.set(type, enabled);
        return this;
    }

    public boolean canSendWebhook(NotificationType type) {
        ConfigMap notifications = (ConfigMap) discordWebhook.get("notifications");

        boolean crashedServers = Boolean.parseBoolean(notifications.get("crashed_servers").toString());
        boolean timedOutServers = Boolean.parseBoolean(notifications.get("timed_out_servers").toString());
        boolean failedServerStarts = Boolean.parseBoolean(notifications.get("failed_server_starts").toString());
        boolean failedPlayerJoins = Boolean.parseBoolean(notifications.get("failed_player_joins").toString());
        boolean playerKicks = Boolean.parseBoolean(notifications.get("player_kicks").toString());

        return switch (type) {
            case SERVER_CRASHED -> crashedServers;
            case SERVER_TIMED_OUT, SERVER_STOP_TIMED_OUT -> timedOutServers;
            case SERVER_START_FAILED -> failedServerStarts;
            case PLAYER_JOIN_FAILED -> failedPlayerJoins;
            case PLAYER_KICKED -> playerKicks;
            default -> false;
        };
    }

    public LogSettingsConfig setPlayerLogCategory(String category, boolean console, boolean inGame) {
        assertCategory(category);
        playerLogs.set(category, new ConfigMap()
                .set("console", console)
                .set("in_game", inGame)
        );
        return this;
    }

    private boolean getPlayerLogValue(String category, String key) {
        ConfigMap configMap = (ConfigMap) playerLogs.get(category);
        return configMap.get(key) != null && (boolean) configMap.get(key);
    }

    public boolean canNotify(NotificationType type) {
        boolean connectionLifecycle = getPlayerLogValue(CATEGORY_CONNECTION, "in_game");
        boolean failedJoins = getPlayerLogValue(CATEGORY_FAILED_JOINS, "in_game");
        boolean kicks = getPlayerLogValue(CATEGORY_KICKS, "in_game");
        boolean serverSwitched = getPlayerLogValue(CATEGORY_SERVER_SWITCHED, "in_game");

        return switch (type) {
            case PLAYER_JOINED, PLAYER_LEFT -> connectionLifecycle;
            case PLAYER_JOIN_FAILED -> failedJoins;
            case PLAYER_KICKED -> kicks;
            case PLAYER_SWITCHED_SERVER -> serverSwitched;
            default -> true;
        };
    }

    public boolean canLog(NotificationType type) {
        boolean connectionLifecycle = getPlayerLogValue(CATEGORY_CONNECTION, "console");
        boolean failedJoins = getPlayerLogValue(CATEGORY_FAILED_JOINS, "console");
        boolean kicks = getPlayerLogValue(CATEGORY_KICKS, "console");
        boolean serverSwitched = getPlayerLogValue(CATEGORY_SERVER_SWITCHED, "console");

        return switch (type) {
            case PLAYER_JOINED, PLAYER_LEFT -> connectionLifecycle;
            case PLAYER_JOIN_FAILED -> failedJoins;
            case PLAYER_KICKED -> kicks;
            case PLAYER_SWITCHED_SERVER -> serverSwitched;
            default -> true;
        };
    }

    public LinkedHashMap<String, Object> getDiscordWebhook() {
        return discordWebhook.toRawMap();
    }

    public LinkedHashMap<String, Object> getPlayerLogs() {
        return playerLogs.toRawMap();
    }
}