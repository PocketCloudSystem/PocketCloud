package de.pocketcloud.shared.event.player;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import lombok.Getter;

@Getter
public final class PlayerJoinedEvent extends PlayerEvent {

    private final ICloudServer server;

    public PlayerJoinedEvent(ICloudPlayer player, ICloudServer server) {
        super(player);
        this.server = server;
    }

    public ICloudPlayer getPlayer() {
        return player;
    }
}