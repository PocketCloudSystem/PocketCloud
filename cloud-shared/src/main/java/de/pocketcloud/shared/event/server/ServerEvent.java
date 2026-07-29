package de.pocketcloud.shared.event.server;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.event.Event;

public abstract class ServerEvent implements Event {

    protected final ICloudServer server;

    public ServerEvent(ICloudServer server) {
        this.server = server;
    }
}