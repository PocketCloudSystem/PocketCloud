package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;

public final class ServerStopEvent extends ServerEvent {

    @Getter
    private final boolean force;

    public ServerStopEvent(CloudServer server, boolean force) {
        super(server);
        this.force = force;
    }
}
