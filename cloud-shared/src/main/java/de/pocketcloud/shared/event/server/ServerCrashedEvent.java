package de.pocketcloud.shared.event.server;

import de.pocketcloud.api.component.server.ICloudServer;

public final class ServerCrashedEvent extends ServerEvent {

    public ServerCrashedEvent(ICloudServer server) {
        super(server);
    }

    public ICloudServer getServer() {
        return server;
    }
}