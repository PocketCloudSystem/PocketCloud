package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;

public final class ServerStartFailedEvent extends ServerEvent {

    public ServerStartFailedEvent(CloudServer server) {
        super(server);
    }
}
