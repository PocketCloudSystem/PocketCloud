package de.pocketcloud.cloud.event.impl.player;

import de.pocketcloud.cloud.event.Event;
import de.pocketcloud.cloud.player.CloudPlayer;
import lombok.Getter;

public abstract class PlayerEvent extends Event {

    @Getter
    private final CloudPlayer player;

    public PlayerEvent(CloudPlayer player) {
        this.player = player;
    }
}
