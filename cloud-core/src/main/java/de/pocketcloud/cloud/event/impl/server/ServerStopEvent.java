package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;

@Getter
public class ServerStopEvent extends ServerEvent {

    private final boolean forcefully;

    public ServerStopEvent(CloudServer server, boolean forcefully) {
        super(server);
        this.forcefully = forcefully;
    }
}