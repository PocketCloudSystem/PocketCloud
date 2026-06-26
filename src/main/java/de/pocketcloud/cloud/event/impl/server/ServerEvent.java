package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.event.Event;
import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;

public abstract class ServerEvent extends Event {

    @Getter
    private final CloudServer server;

    public ServerEvent(CloudServer server) {
        this.server = server;
    }
}