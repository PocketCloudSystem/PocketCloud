package de.pocketcloud.shared.event.player;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import lombok.Getter;

@Getter
public final class PlayerKickedEvent extends PlayerEvent {

    private final ICloudServer server;
    private final String reason;

    public PlayerKickedEvent(ICloudPlayer player, ICloudServer server, String reason) {
        super(player);
        this.server = server;
        this.reason = reason;
    }

    public ICloudPlayer getPlayer() {
        return player;
    }
}