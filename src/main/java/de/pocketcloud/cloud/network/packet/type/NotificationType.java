package de.pocketcloud.cloud.network.packet.type;

import de.pocketcloud.cloud.config.impl.LogSettingsConfig;
import de.pocketcloud.cloud.config.impl.MainConfig;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.language.LanguageKey;
import de.pocketcloud.cloud.network.packet.impl.CloudNotificationPacket;
import de.pocketcloud.cloud.server.CloudServerManager;
import de.pocketcloud.cloud.server.util.ServerCrashData;
import de.pocketcloud.cloud.template.TemplateType;
import de.pocketcloud.cloud.util.Writable;
import de.pocketcloud.cloud.util.concurrent.Promise;
import de.r3pt1s.discord.webhook.Webhook;
import de.r3pt1s.discord.webhook.message.Message;
import de.r3pt1s.discord.webhook.message.embed.Embed;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public enum NotificationType implements Writable<String> {
    
    SERVER_STARTING,
    SERVER_STOPPING,
    SERVER_TIMED_OUT,
    SERVER_STOP_TIMED_OUT,
    SERVER_CRASHED,
    SERVER_START_FAILED,
    PLAYER_JOINED,
    PLAYER_LEFT,
    PLAYER_JOIN_FAILED,
    PLAYER_KICKED,
    PLAYER_SWITCHED_SERVER;

    public boolean notify(Map<String, Object> args, Map<Object, Object> extraArgs) {
        if (!canNotify()) return false;
        if (canSendWebhook()) {
            Message message = craftDiscordMessage(args, extraArgs);
            Webhook webhook = LogSettingsConfig.instance().craftDiscordWebhook();
            if (message != null && webhook != null) {
                message.sendWithDiffWebhook(webhook).exceptionally(ex -> {
                    CloudLogger.get().exception("§cFailed to spread notification to discord", ex);
                    return null;
                }).thenAccept(res -> {
                    if (!res.isSuccess()) {
                        CloudLogger.get().error("§cFailed to spread notification to discord responded with code §e{}§8: §e{}", res.statusCode(), res.body());
                    }
                });
            }
        }

        CloudNotificationPacket.create(this, args).broadcastPacket(CloudServerManager.instance().getAll(TemplateType.PROXY).isEmpty() ? TemplateType.PROXY : TemplateType.SERVER);
        return true;
    }

    public Message craftDiscordMessage(Map<String, Object> args, Map<Object, Object> extraArgs) {
        Message message = new Message().wait(false);
        message.setUsername("PocketCloud Notifications | " + MainConfig.instance().getCloudName());
        message.setAvatarUrl("https://avatars.githubusercontent.com/u/97796660?s=400&u=a65bced92fb37ce5bafc5f1eff9e2845fe66a9cb&v=4");
        switch (this) {
            case SERVER_CRASHED -> {
                ServerCrashData crashData = ServerCrashData.read((Map<String, Object>) extraArgs.getOrDefault("crashData", Map.of()));
                String line = crashData.line() == null ? "No line found." : crashData.line().toString();
                message.addEmbed(Embed.create()
                        .setTitle("Notification | Server Crash Report")
                        .setDescription("`The cloud detected a crash on the following server:`")
                        .setColor(Color.RED)
                        .addField("**Affected Server**", "> " + args.getOrDefault("server", "Unknown"), false)
                        .setFooter("Notification Type: " + name(), null, null)
                ).addEmbedIf(() -> crashData == null, Embed.create()
                        .setTitle("Crash Data")
                        .setDescription("> No data available.")
                        .setColor(Color.RED)
                ).addEmbedIf(() -> crashData != null, Embed.create()
                        .setTitle("Crash Data")
                        .addField("**Error Type**", "> " + Optional.ofNullable(crashData.type()).orElse("No error type found."), true)
                        .addField("**File**", "> " + Optional.ofNullable(crashData.file()).orElse("No file found.") + " (L: " + line + ")", true)
                        .addField("**Message**", "> " + Optional.ofNullable(crashData.message()).orElse("No message found."), false)
                        .addField("**Trace**", "> " + String.join("\n", Optional.ofNullable(crashData.stackTrace()).orElse(List.of())).substring(0, 1000), false)
                );
            }
            case SERVER_START_FAILED -> {
                String reason = (String) args.get("reason");
                if (reason == null) {
                    message.addEmbed(Embed.create()
                            .setTitle("Notification | Server Start Failed")
                            .setDescription("`The server exceeded the time to start, killed the created process. (Please take a look into this)`")
                            .setColor(Color.RED)
                            .addField("**Affected Server**", "> " + args.get("server"), false)
                            .setFooter("Notification Type: " + this.name(), null, null)
                    );
                } else {
                    message.addEmbed(Embed.create()
                            .setTitle("Notification | Server Start Failed")
                            .setDescription("`The server failed to start.`")
                            .setColor(Color.RED)
                            .addField("**Affected Server**", "> " + args.get("server"), false)
                            .addField("**Reason**", "> " + reason, false)
                            .setFooter("Notification Type: " + this.name(), null, null)
                    );
                }
            }
            case SERVER_STOP_TIMED_OUT -> message.addEmbed(Embed.create()
                    .setTitle("Notification | Server Timeout")
                    .setDescription("`The server exceeded the time to stop, killed the process.`")
                    .setColor(Color.RED)
                    .addField("**Affected Server**", "> " + args.get("server"), false)
                    .setFooter("Notification Type: " + this.name(), null, null)
            );
            case SERVER_TIMED_OUT -> message.addEmbed(Embed.create()
                    .setTitle("Notification | Server Timeout")
                    .setDescription("`The server did not respond to the cloud ping, killed the process.`")
                    .setColor(Color.RED)
                    .addField("**Affected Server**", "> " + args.get("server"), false)
                    .setFooter("Notification Type: " + this.name(), null, null)
            );
            case PLAYER_JOIN_FAILED -> {
                String player = (String) args.get("player");
                String server = (String) args.get("server");
                String reason = (String) args.get("reason");
                message.addEmbed(Embed.create()
                        .setTitle("Notification | Player Join Failed")
                        .setDescription("`The player tried to join but has been kicked during his login/ join.`")
                        .setColor(Color.RED)
                        .addField("**Affected Player**", "> " + player, false)
                        .addField("**Initial Server**", "> " + server, false)
                        .addField("**Reason**", "> " + ((reason == null || reason.isEmpty()) ? "No reason applied." : reason), false)
                        .setFooter("Notification Type: " + this.name(), null, null)
                );
            }
            case PLAYER_KICKED -> {
                String player = (String) args.get("player");
                String server = (String) args.get("server");
                String reason = (String) args.get("reason");
                message.addEmbed(Embed.create()
                        .setTitle("Notification | Player Kicked")
                        .setDescription("`The player has been kicked.`")
                        .setColor(Color.RED)
                        .addField("**Affected Player**", "> " + player, false)
                        .addField("**Server**", "> " + server, false)
                        .addField("**Reason**", "> " + ((reason == null || reason.isEmpty()) ? "No reason applied." : reason), false)
                        .setFooter("Notification Type: " + this.name(), null, null)
                );
            }
            default -> message = null;
        }

        return message;
    }

    public boolean canSendWebhook() {
        return LogSettingsConfig.instance().canSendWebhook(this);
    }

    public boolean canNotify() {
        return LogSettingsConfig.instance().canNotify(this);
    }

    public boolean canLog() {
        return LogSettingsConfig.instance().canLog(this);
    }

    public LanguageKey langKey() {
        return switch (this) {
            case SERVER_STARTING -> LanguageKey.INGAME_NOTIFY_MESSAGE_SERVER_STARTING;
            case SERVER_STOPPING -> LanguageKey.INGAME_NOTIFY_MESSAGE_SERVER_STOPPING;
            case SERVER_TIMED_OUT -> LanguageKey.INGAME_NOTIFY_MESSAGE_SERVER_TIMED_OUT;
            case SERVER_STOP_TIMED_OUT -> LanguageKey.INGAME_NOTIFY_MESSAGE_SERVER_STOP_TIMED_OUT;
            case SERVER_CRASHED -> LanguageKey.INGAME_NOTIFY_MESSAGE_SERVER_CRASHED;
            case SERVER_START_FAILED -> LanguageKey.INGAME_NOTIFY_MESSAGE_SERVER_START_FAILED;
            case PLAYER_JOINED -> LanguageKey.INGAME_NOTIFY_MESSAGE_PLAYER_JOINED;
            case PLAYER_LEFT -> LanguageKey.INGAME_NOTIFY_MESSAGE_PLAYER_LEFT;
            case PLAYER_JOIN_FAILED -> LanguageKey.INGAME_NOTIFY_MESSAGE_PLAYER_JOIN_FAILED;
            case PLAYER_KICKED -> LanguageKey.INGAME_NOTIFY_MESSAGE_PLAYER_KICKED;
            case PLAYER_SWITCHED_SERVER -> LanguageKey.INGAME_NOTIFY_MESSAGE_PLAYER_SWITCHED_SERVER;
        };
    }

    @Override
    public String write() {
        return name();
    }
}