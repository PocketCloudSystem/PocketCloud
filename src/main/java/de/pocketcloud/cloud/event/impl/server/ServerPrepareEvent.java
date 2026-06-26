package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;

public final class ServerPrepareEvent extends ServerEvent {

    public ServerPrepareEvent(CloudServer server) {
        super(server);
    }
}