package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;

import java.util.List;

public class ServerCrashEvent extends ServerEvent {

    @Getter
    private final List<String> data;

    public ServerCrashEvent(CloudServer server, List<String> data) {
        super(server);
        this.data = data;
    }
}
