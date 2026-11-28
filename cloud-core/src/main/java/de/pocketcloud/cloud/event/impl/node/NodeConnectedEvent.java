package de.pocketcloud.cloud.event.impl.node;

import de.pocketcloud.cloud.event.CloudEvent;
import lombok.Getter;

/** Another node of the cluster connected to this node (also after a reconnect). */
@Getter
public class NodeConnectedEvent extends CloudEvent {

    private final String node;

    public NodeConnectedEvent(String node) {
        this.node = node;
    }
}
