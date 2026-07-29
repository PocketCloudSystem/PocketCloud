package de.pocketcloud.shared.event.player;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public final class PlayerTransferredEvent extends PlayerEvent {

    @Nullable
    private final ICloudServer oldServer;
    private final ICloudServer newServer;

    public PlayerTransferredEvent(ICloudPlayer player, @Nullable ICloudServer oldServer, ICloudServer newServer) {
        super(player);
        this.oldServer = oldServer;
        this.newServer = newServer;
    }

    public ICloudPlayer getPlayer() {
        return player;
    }
}