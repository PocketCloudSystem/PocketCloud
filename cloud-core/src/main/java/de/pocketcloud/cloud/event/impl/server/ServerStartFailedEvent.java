package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;

public class ServerStartFailedEvent extends ServerEvent {

    public ServerStartFailedEvent(CloudServer server) {
        super(server);
    }
}
