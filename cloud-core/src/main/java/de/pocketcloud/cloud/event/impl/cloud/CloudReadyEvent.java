package de.pocketcloud.cloud.event.impl.cloud;

import de.pocketcloud.cloud.event.Event;
import lombok.Getter;

import java.time.Instant;

public class CloudReadyEvent extends Event {

    @Getter
    private final Instant time;

    public CloudReadyEvent(Instant time) {
        this.time = time;
    }
}
