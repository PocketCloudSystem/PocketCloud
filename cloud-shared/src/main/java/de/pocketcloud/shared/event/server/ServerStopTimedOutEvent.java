package de.pocketcloud.shared.event.server;

import de.pocketcloud.api.component.server.ICloudServer;

public final class ServerStopTimedOutEvent extends ServerEvent {

    public ServerStopTimedOutEvent(ICloudServer server) {
        super(server);
    }

    public ICloudServer getServer() {
        return server;
    }
}