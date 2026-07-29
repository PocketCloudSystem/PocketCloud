package de.pocketcloud.cloud.event.impl.player;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.player.CloudPlayer;
import lombok.Getter;

/**
 * This event is called when you kick a CloudPlayer directly via the cloud with {@link CloudPlayer#kick()}
 */

@Getter
public class PlayerKickEvent extends PlayerEvent implements Cancelable {

    private final String reason;
    private final String disconnectScreenMessage;

    public PlayerKickEvent(CloudPlayer player, String reason, String disconnectScreenMessage) {
        super(player);
        this.reason = reason;
        this.disconnectScreenMessage = disconnectScreenMessage;
    }
}