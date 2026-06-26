package de.pocketcloud.cloud.event.impl.player;

import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;

public final class PlayerConnectEvent extends PlayerEvent {

    @Getter
    private final CloudServer server;

    public PlayerConnectEvent(CloudPlayer player, CloudServer server) {
        super(player);
        this.server = server;
    }
}
