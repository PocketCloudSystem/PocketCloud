package de.pocketcloud.cloud.util.benchmark;

import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public final class BenchmarkTiming {

    private final String name;
    private final long currentTick;

    private boolean running = false;
    private long start = 0;
    private long end = 0;
    private Double duration = null;

    public BenchmarkTiming(String name, long currentTick) {
        this.name = name;
        this.currentTick = currentTick;
    }

    public void startTiming() {
        if (running) return;
        running = true;
        start = System.nanoTime();
    }

    public void stopTiming() {
        if (!running) return;
        running = false;
        end = System.nanoTime();
        duration = (end - start) / 1_000_000.0;
    }

    public boolean isDone() { return duration != null; }

    public double startInMs() { return start / 1_000_000.0; }

    public double endInMs() { return end / 1_000_000.0; }
}