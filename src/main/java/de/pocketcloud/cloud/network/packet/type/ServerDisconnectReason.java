package de.pocketcloud.cloud.network.packet.type;

import de.pocketcloud.cloud.util.Writable;

public enum ServerDisconnectReason implements Writable<String> {

    CLOUD_SHUTDOWN,
    SERVER_SHUTDOWN;

    @Override
    public String write() {
        return name();
    }
}