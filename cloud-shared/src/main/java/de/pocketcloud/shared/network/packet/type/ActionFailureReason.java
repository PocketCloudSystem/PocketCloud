package de.pocketcloud.shared.network.packet.type;

import de.pocketcloud.common.serialization.Writable;

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