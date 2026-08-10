package de.pocketcloud.cloud.tick;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.common.lifecycle.Tickable;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

@Getter
@Accessors(fluent = true)
public final class Ticker {

    public static final long TICK_RATE_MS = 50;
    public static final long MAX_SLEEP_MS = 100;
    public static final long DRIFT_WARNING_THRESHOLD_MS = 200;
    public static final long DRIFT_WARNING_INTERVAL_MS = 5_000;

    private final Map<String, Tickable> tickableList = new HashMap<>();

    private long tickCounter = 0;
    private long nextTick = 0;
    private long lastSleepDriftWarning = 0;

    public Ticker register(Tickable tickable) {
        if (tickableList.containsKey(tickable.getClass().getName()))
            throw new IllegalArgumentException("Tickable already exists");
        tickableList.put(tickable.getClass().getName(), tickable);
        return this;
    }

    public Ticker registerAll(Tickable... tickables) {
        for (Tickable tickable : tickables) register(tickable);
        return this;
    }

    public Ticker unregister(Class<Tickable> tickable) {
        tickableList.remove(tickable.getName());
        return this;
    }

    public Ticker unregister(Tickable tickable) {
        tickableList.remove(tickable.getClass().getName());
        return this;
    }

    public void tick() {
        this.nextTick = System.currentTimeMillis();
        while (PocketCloud.instance().running()) {
            long tickStart = System.currentTimeMillis();
            if ((tickStart - this.nextTick) < -25) {
                sleepUntilNextTick();
                continue;
            }

            Benchmark.startTiming("cloud_tick");
            this.tickCounter++;

            if (!PocketCloud.instance().loader().isReloading()) {
                for (Tickable tickable : tickableList.values()) {
                    Benchmark.startTiming("tick_" + tickable.getClass().getName());
                    tickable.tick(tickCounter);
                    Benchmark.stopTiming("tick_" + tickable.getClass().getName());
                }
            }

            long tickEnd = System.currentTimeMillis();
            Benchmark.stopTiming("cloud_tick");

            if ((this.nextTick - tickStart) < -1000) {
                this.nextTick = tickStart;
            } else {
                this.nextTick += TICK_RATE_MS;
            }

            PocketCloud.instance().performanceStats().updateTickStats(tickStart / 1000.0, tickEnd / 1000.0);
            PocketCloud.instance().performanceStats().updateSystemStats(tickCounter);

            sleepUntilNextTick();
        }
    }

    private void sleepUntilNextTick() {
        long now = System.currentTimeMillis();
        long delay = this.nextTick - now;
        if (delay <= 0) return;

        if (delay > MAX_SLEEP_MS) {
            if (delay >= DRIFT_WARNING_THRESHOLD_MS && (now - lastSleepDriftWarning) >= DRIFT_WARNING_INTERVAL_MS) {
                lastSleepDriftWarning = now;
                CloudLogger.get().warn("Main thread sleep drift detected: next tick was §e{}ms §rin the future, capping sleep.", delay);
            }
            this.nextTick = now + MAX_SLEEP_MS;
            delay = MAX_SLEEP_MS;
        }

        LockSupport.parkNanos(delay * 1_000_000L);
    }
}