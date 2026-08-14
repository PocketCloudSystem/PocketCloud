package de.pocketcloud.cloud.event.impl.cloud;

import de.pocketcloud.cloud.event.CloudEvent;
import lombok.Getter;

import java.time.Instant;

public class CloudReadyEvent extends CloudEvent {

    @Getter
    private final Instant time;

    public CloudReadyEvent(Instant time) {
        this.time = time;
    }
}