package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;

public final class ServerPostVerificationEvent extends ServerEvent {

    public ServerPostVerificationEvent(CloudServer server) {
        super(server);
    }
}
