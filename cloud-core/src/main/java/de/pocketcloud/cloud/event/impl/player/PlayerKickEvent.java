package de.pocketcloud.cloud.event.impl.player;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.player.CloudPlayer;
import lombok.Getter;

public class PlayerKickEvent extends PlayerEvent implements Cancelable {

    @Getter
    private final String reason;
    @Getter
    private final String disconnectScreenMessage;

    public PlayerKickEvent(CloudPlayer player, String reason, String disconnectScreenMessage) {
        super(player);
        this.reason = reason;
        this.disconnectScreenMessage = disconnectScreenMessage;
    }
}
