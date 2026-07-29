package de.pocketcloud.shared.event.server;

import de.pocketcloud.api.component.server.ICloudServer;

public final class ServerDisconnectedEvent extends ServerEvent {

    public ServerDisconnectedEvent(ICloudServer server) {
        super(server);
    }
}