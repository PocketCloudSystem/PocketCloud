package de.pocketcloud.cloud.event.impl.server;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;

public class ServerSendCommandEvent extends ServerEvent implements Cancelable {

    @Getter
    private final String commandLine;

    public ServerSendCommandEvent(CloudServer server, String commandLine) {
        super(server);
        this.commandLine = commandLine;
    }
}
