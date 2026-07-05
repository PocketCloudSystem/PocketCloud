package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;

public class ServerVerifyEvent extends ServerEvent {

    public ServerVerifyEvent(CloudServer server) {
        super(server);
    }
}