package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.event.CloudEvent;
import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;

public abstract class ServerEvent extends CloudEvent {

    @Getter
    private final CloudServer server;

    public ServerEvent(CloudServer server) {
        this.server = server;
    }
}