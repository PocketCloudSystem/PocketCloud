package de.pocketcloud.cloud.event.impl.player;

import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;

public final class PlayerDisconnectEvent extends PlayerEvent {

    @Getter
    private final CloudServer server;
    @Getter
    private final String serverName;

    public PlayerDisconnectEvent(CloudPlayer player, CloudServer server, String serverName) {
        super(player);
        this.server = server;
        this.serverName = serverName;
    }
}
