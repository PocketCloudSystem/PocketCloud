package de.pocketcloud.cloud.event.impl.node;

import de.pocketcloud.cloud.event.CloudEvent;
import lombok.Getter;

/** Another node left the cluster or is considered dead; its servers are treated as offline. */
@Getter
public class NodeDisconnectedEvent extends CloudEvent {

    private final String node;

    public NodeDisconnectedEvent(String node) {
        this.node = node;
    }
}
