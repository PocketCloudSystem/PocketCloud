package de.pocketcloud.shared.event.server;

import de.pocketcloud.api.component.server.ICloudServer;

public final class ServerStartingEvent extends ServerEvent {

    public ServerStartingEvent(ICloudServer server) {
        super(server);
    }

    public ICloudServer getServer() {
        return server;
    }
}