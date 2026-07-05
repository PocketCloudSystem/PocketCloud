package de.pocketcloud.cloud.network.packet.type;

import de.pocketcloud.common.serialization.Writable;

public enum ServerDisconnectReason implements Writable<String> {

    CLOUD_SHUTDOWN,
    SERVER_SHUTDOWN;

    @Override
    public String write() {
        return name();
    }
}