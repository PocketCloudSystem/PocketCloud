package de.pocketcloud.cloud.util;

import de.pocketcloud.cloud.PocketCloud;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Samples players, servers, CPU and memory of this node once a minute for the last 24 hours (web panel charts).
 * shortcut: kept in memory only, so a restart starts an empty history; persist it if longer ranges are wanted.
 */
public final class StatsHistory {

    public static final long INTERVAL_MS = 60_000;
    private static final int MAX_POINTS = 24 * 60;

    public record Point(long time, int players, int servers, double cpu, long memory) {}

    private static final ArrayDeque<Point> POINTS = new ArrayDeque<>();
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private StatsHistory() {}

    public static void start() {
        if (!STARTED.compareAndSet(false, true)) return;
        Thread.ofVirtual().name("PocketCloud-StatsHistory").start(() -> {
            while (true) {
                try {
                    Thread.sleep(INTERVAL_MS);
                    sample();
                } catch (InterruptedException e) {
                    return;
                } catch (RuntimeException ignored) {
                    // cloud not fully up or shutting down: skip this sample
                }
            }
        });
    }

    private static void sample() {
        PocketCloud cloud = PocketCloud.instance();
        PerformanceStats stats = cloud.performanceStats();
        add(new Point(System.currentTimeMillis(), cloud.players().playerCount(), cloud.servers().serverCount(),
                stats.processCpuUsage(), (long) stats.processUsedMemory()));
    }

    static synchronized void add(Point point) {
        POINTS.addLast(point);
        while (POINTS.size() > MAX_POINTS) POINTS.removeFirst();
    }

    public static synchronized List<Point> points() {
        return new ArrayList<>(POINTS);
    }
}
