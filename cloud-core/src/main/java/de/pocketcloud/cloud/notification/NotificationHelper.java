package de.pocketcloud.cloud.notification;

import de.pocketcloud.api.language.LanguageKey;
import de.pocketcloud.shared.network.packet.type.NotificationType;

public final class NotificationHelper {

    public static LanguageKey toLangKey(NotificationType type) {
        return switch (type) {
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
}