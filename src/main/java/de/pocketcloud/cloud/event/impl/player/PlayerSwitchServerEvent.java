package de.pocketcloud.cloud.event.impl.player;

import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

public final class PlayerSwitchServerEvent extends PlayerEvent {

    @Getter
    @Nullable
    private final CloudServer oldServer;
    @Getter
    private final CloudServer newServer;

    public PlayerSwitchServerEvent(CloudPlayer player, @Nullable CloudServer oldServer, CloudServer newServer) {
        super(player);
        this.oldServer = oldServer;
        this.newServer = newServer;
    }
}
