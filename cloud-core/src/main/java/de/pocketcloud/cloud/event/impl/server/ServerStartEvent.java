package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;

public class ServerStartEvent extends ServerEvent {

    public ServerStartEvent(CloudServer server) {
        super(server);
    }
}