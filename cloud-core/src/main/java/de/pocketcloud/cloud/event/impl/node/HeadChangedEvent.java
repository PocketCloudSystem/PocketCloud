package de.pocketcloud.cloud.event.impl.node;

import de.pocketcloud.cloud.event.CloudEvent;
import lombok.Getter;

/** A node became the head of the cluster. {@code previousHead} is null if no head was known before. */
@Getter
public class HeadChangedEvent extends CloudEvent {

    private final String head;
    private final String previousHead;
    private final boolean thisNode;

    public HeadChangedEvent(String head, String previousHead, boolean thisNode) {
        this.head = head;
        this.previousHead = previousHead;
        this.thisNode = thisNode;
    }
}
