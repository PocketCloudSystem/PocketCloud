package de.pocketcloud.shared.event.server;

import de.pocketcloud.api.component.server.ICloudServer;

public final class ServerVerifiedEvent extends ServerEvent {

    public ServerVerifiedEvent(ICloudServer server) {
        super(server);
    }

    public ICloudServer getServer() {
        return server;
    }
}