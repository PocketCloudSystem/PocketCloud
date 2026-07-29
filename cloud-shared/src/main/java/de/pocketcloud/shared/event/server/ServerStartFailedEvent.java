package de.pocketcloud.shared.event.server;

import de.pocketcloud.api.component.server.ICloudServer;
import lombok.Getter;

@Getter
public final class ServerStartFailedEvent extends ServerEvent {

    private final String reason;

    public ServerStartFailedEvent(ICloudServer server, String reason) {
        super(server);
        this.reason = reason;
    }

    public ICloudServer getServer() {
        return server;
    }
}