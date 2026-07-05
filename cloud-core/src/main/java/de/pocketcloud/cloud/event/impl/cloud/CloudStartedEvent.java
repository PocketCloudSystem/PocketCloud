package de.pocketcloud.cloud.event.impl.cloud;

import de.pocketcloud.cloud.event.Event;
import lombok.Getter;

public final class CloudStartedEvent extends Event {

    @Getter
    private final double time;

    public CloudStartedEvent(double time) {
        this.time = time;
    }
}
