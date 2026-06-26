package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;

public final class ServerTimeOutEvent extends ServerEvent {

    public ServerTimeOutEvent(CloudServer server) {
        super(server);
    }
}
