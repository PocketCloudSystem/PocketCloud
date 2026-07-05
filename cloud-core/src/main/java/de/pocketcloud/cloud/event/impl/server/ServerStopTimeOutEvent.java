package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;

public final class ServerStopTimeOutEvent extends ServerEvent {

    public ServerStopTimeOutEvent(CloudServer server) {
        super(server);
    }
}
