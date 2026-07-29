package de.pocketcloud.cloud.event;

import de.pocketcloud.api.event.Event;
import de.pocketcloud.api.event.EventService;
import de.pocketcloud.cloud.plugin.CloudPlugin;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import org.jetbrains.annotations.ApiStatus;

public final class EventManager extends EventService<CloudPlugin> {

    @Override
    @ApiStatus.Internal
    public void call(Event event) {
        Benchmark.startTiming("event_" + event.getClass().getName());
        try {
            super.call(event);
        } finally {
            try {
                Benchmark.stopTiming("event_" + event.getClass().getName());
            } catch (RuntimeException _) {}
        }
    }
}