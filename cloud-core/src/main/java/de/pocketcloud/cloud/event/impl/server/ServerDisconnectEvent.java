package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;

public final class ServerDisconnectEvent extends ServerEvent {

    public ServerDisconnectEvent(CloudServer server) {
        super(server);
    }
}
