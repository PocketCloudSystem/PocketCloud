package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.crash.CrashData;
import lombok.Getter;

@Getter
public class ServerCrashedEvent extends ServerEvent {

    private final CrashData data;

    public ServerCrashedEvent(CloudServer server, CrashData data) {
        super(server);
        this.data = data;
    }
}