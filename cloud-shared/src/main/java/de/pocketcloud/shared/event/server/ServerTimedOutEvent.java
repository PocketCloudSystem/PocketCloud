package de.pocketcloud.shared.event.server;

import de.pocketcloud.api.component.server.ICloudServer;

public final class ServerTimedOutEvent extends ServerEvent {

    public ServerTimedOutEvent(ICloudServer server) {
        super(server);
    }

    public ICloudServer getServer() {
        return server;
    }
}