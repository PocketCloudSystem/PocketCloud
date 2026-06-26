package de.pocketcloud.cloud.server.util;

import de.pocketcloud.cloud.util.Writable;
import lombok.Getter;

public enum ServerStatus implements Writable<String> {

    PENDING("§gPENDING"),
    STARTING("§2STARTING"),
    ONLINE("§aONLINE"),
    FULL("§eFULL"),
    IN_GAME("§6INGAME"),
    STOPPING("§4STOPPING"),
    OFFLINE("§cOFFLINE");

    @Getter
    private final String display;

    ServerStatus(String display) {
        this.display = display;
    }

    public boolean isPending() {
        return this == PENDING;
    }

    public boolean isStarting() {
        return this == STARTING;
    }

    public boolean isOnline() {
        return isOnline(false);
    }

    public boolean isOnline(boolean literal) {
        return this == ONLINE || (!literal && (isFull() || isInGame()));
    }

    public boolean isFull() {
        return this == FULL;
    }

    public boolean isInGame() {
        return this == IN_GAME;
    }

    public boolean isStopping() {
        return this == STOPPING;
    }

    public boolean isOffline() {
        return this == OFFLINE;
    }

    @Override
    public String write() {
        return name();
    }
}