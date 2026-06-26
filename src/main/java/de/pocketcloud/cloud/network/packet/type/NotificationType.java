package de.pocketcloud.cloud.network.packet.type;

import de.pocketcloud.cloud.config.impl.LogSettingsConfig;
import de.pocketcloud.cloud.language.LanguageKey;
import de.pocketcloud.cloud.util.Writable;
import de.pocketcloud.cloud.util.concurrent.Promise;

import java.util.Map;

public enum NotificationType implements Writable<String> {
    
    SERVER_STARTING {


    },
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

    public Promise<Void> notify(Map<String, Object> args, Object... extraArgs) {
        return Promise.resolved(null); //TODO
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