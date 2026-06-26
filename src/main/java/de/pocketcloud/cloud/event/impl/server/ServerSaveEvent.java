package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.server.CloudServer;

public final class ServerSaveEvent extends ServerEvent implements Cancelable {

    public ServerSaveEvent(CloudServer server) {
        super(server);
    }
}
