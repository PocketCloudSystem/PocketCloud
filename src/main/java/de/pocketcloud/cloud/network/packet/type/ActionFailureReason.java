package de.pocketcloud.cloud.network.packet.type;

import de.pocketcloud.cloud.util.Writable;

public enum ActionFailureReason implements Writable<String> {

    NONE,
    TEMPLATE_NOT_FOUND,
    MAX_SERVERS_REACHED,
    SERVER_NOT_FOUND,
    REQUEST_TIMEOUT;

    @Override
    public String write() {
        return name();
    }
}