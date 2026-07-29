package de.pocketcloud.cloud.event;

import de.pocketcloud.api.event.Event;
import de.pocketcloud.cloud.PocketCloud;
import lombok.Getter;

public abstract class CloudEvent implements Event {

    public final int MAX_EVENT_CALL_DEPTH = 50;
    private static int eventCallDepth = 1;
    @Getter
    private boolean cancelled = false;

    public void cancel() {
        if (!(this instanceof Cancelable)) throw new IllegalStateException("Event is not cancelable");
        this.cancelled = true;
    }

    public void uncancel() {
        if (!(this instanceof Cancelable)) throw new IllegalStateException("Event is not cancelable");
        this.cancelled = false;
    }

    public CloudEvent call() {
        if (eventCallDepth >= MAX_EVENT_CALL_DEPTH) {
            throw new RuntimeException("Recursive event call detected (reached max depth of " + MAX_EVENT_CALL_DEPTH + " calls)");
        }

        ++eventCallDepth;
        try {
            PocketCloud.instance().events().call(this);
        } finally {
            --eventCallDepth;
        }

        return this;
    }

    public String name() {
        return getClass().getSimpleName();
    }
}