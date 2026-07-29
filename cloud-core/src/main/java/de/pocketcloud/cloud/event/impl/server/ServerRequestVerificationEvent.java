package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.server.CloudServer;

public class ServerRequestVerificationEvent extends ServerEvent implements Cancelable {

    public ServerRequestVerificationEvent(CloudServer server) {
        super(server);
    }
}