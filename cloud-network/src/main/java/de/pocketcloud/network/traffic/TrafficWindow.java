package de.pocketcloud.network.traffic;

import de.pocketcloud.common.util.TimeUtils;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

public final class TrafficWindow {

    private final Deque<Sample> samples = new ConcurrentLinkedDeque<>();
    private final AtomicLong total = new AtomicLong();
    private final double windowSeconds;

    public TrafficWindow(double windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public void push(long bytes) {
        total.addAndGet(bytes);
        samples.add(new Sample(TimeUtils.currentTime(), bytes));
    }

    public void cleanup() {
        double threshold = TimeUtils.currentTime() - windowSeconds;
        Sample first;
        while ((first = samples.peekFirst()) != null && first.time() < threshold) {
            samples.pollFirst();
        }
    }

    public long windowSum() {
        return samples.stream().mapToLong(Sample::bytes).sum();
    }

    public long total() {
        return total.get();
    }

    public record Sample(double time, long bytes) {}
}