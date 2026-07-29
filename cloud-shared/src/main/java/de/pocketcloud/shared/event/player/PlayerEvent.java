package de.pocketcloud.shared.event.player;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.event.Event;

public abstract class PlayerEvent implements Event {

    protected final ICloudPlayer player;

    public PlayerEvent(ICloudPlayer player) {
        this.player = player;
    }
}