package de.pocketcloud.cloud.traffic;

import de.pocketcloud.cloud.util.TimeUtils;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public final class TrafficWindow {

    private final Deque<TrafficSample> samples = new ConcurrentLinkedDeque<>();
    private final AtomicLong total = new AtomicLong();
    private final double windowSeconds;

    public TrafficWindow(double windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public void push(long bytes) {
        total.addAndGet(bytes);
        samples.add(new TrafficSample(TimeUtils.currentTime(), bytes));
    }

    public void cleanup() {
        double threshold = TimeUtils.currentTime() - windowSeconds;
        while (true) {
            TrafficSample first = samples.peekFirst();
            if (first == null || first.time() >= threshold) break;
            samples.pollFirst();
        }
    }

    public long windowSum() {
        return samples.stream().mapToLong(TrafficSample::bytes).sum();
    }

    public long total() {
        return total.get();
    }

    public record TrafficSample(double time, long bytes) {}
}