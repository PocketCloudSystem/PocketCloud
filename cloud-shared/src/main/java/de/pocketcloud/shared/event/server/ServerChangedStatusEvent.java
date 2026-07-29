package de.pocketcloud.shared.event.server;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.server.ServerStatus;
import lombok.Getter;

@Getter
public final class ServerChangedStatusEvent extends ServerEvent {

    private final ServerStatus oldStatus;
    private final ServerStatus newStatus;

    public ServerChangedStatusEvent(ICloudServer server, ServerStatus oldStatus, ServerStatus newStatus) {
        super(server);
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public ICloudServer getServer() {
        return server;
    }
}