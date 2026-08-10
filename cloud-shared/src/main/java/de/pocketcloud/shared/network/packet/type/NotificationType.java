package de.pocketcloud.shared.network.packet.type;

import de.pocketcloud.common.serialization.Writable;

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

    @Override
    public String write() {
        return name();
    }
}