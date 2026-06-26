package de.pocketcloud.cloud.traffic;

import de.pocketcloud.cloud.util.TimeUtils;
import de.pocketcloud.cloud.util.TriConsumer;
import lombok.Getter;

import java.net.SocketAddress;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public abstract class TrafficMonitor {

    public static final String REGULAR_MODE_IN = "in";
    public static final String REGULAR_MODE_OUT = "out";
    public static final String SUFFIX_AVG = "_avg";

    @Getter
    protected final AtomicBoolean active = new AtomicBoolean(true);
    @Getter
    protected final long timestamp;
    protected volatile Long monitoringDuration = null;
    @Getter
    protected final Map<String, List<TriConsumer<SocketAddress, Object, Long>>> handlers = new ConcurrentHashMap<>();
    @Getter
    protected final AtomicLong totalBytesIn = new AtomicLong();
    @Getter
    protected final AtomicLong totalBytesOut = new AtomicLong();
    protected final Deque<TrafficMonitorManager.TrafficData> byteHistoryIn = new ConcurrentLinkedDeque<>();
    protected final Deque<TrafficMonitorManager.TrafficData> byteHistoryOut = new ConcurrentLinkedDeque<>();
    protected volatile Consumer<Object[]> stopMonitoringHandler = null;
    @Getter
    private final String monitorType;

    public TrafficMonitor(String monitorType) {
        this.monitorType = monitorType;
        this.timestamp = TimeUtils.currentSeconds();
    }

    public TrafficMonitor monitorIn(TriConsumer<SocketAddress, Object, Long> handler) {
        addHandler(REGULAR_MODE_IN, handler);
        return this;
    }

    public TrafficMonitor monitorOut(TriConsumer<SocketAddress, Object, Long> handler) {
        addHandler(REGULAR_MODE_OUT, handler);
        return this;
    }

    protected void addHandler(String mode, TriConsumer<SocketAddress, Object, Long> handler) {
        handlers.computeIfAbsent(mode, _ -> new CopyOnWriteArrayList<>())
                .add(handler);
    }

    public void pushBytes(String mode, long bytes) {
        if (!active.get()) return;
        double now = TimeUtils.currentTime();

        switch (mode.toLowerCase()) {
            case REGULAR_MODE_IN -> {
                totalBytesIn.addAndGet(bytes);
                byteHistoryIn.add(new TrafficMonitorManager.TrafficData(now, bytes));
            }
            case REGULAR_MODE_OUT -> {
                totalBytesOut.addAndGet(bytes);
                byteHistoryOut.add(new TrafficMonitorManager.TrafficData(now, bytes));
            }
        }
    }

    public void cleanupHistory() {
        double threshold = TimeUtils.currentTime() - 1.0;

        while (true) {
            TrafficMonitorManager.TrafficData first = byteHistoryIn.peekFirst();

            if (first == null || first.time() >= threshold) {
                break;
            }

            byteHistoryIn.pollFirst();
        }

        while (true) {
            TrafficMonitorManager.TrafficData first = byteHistoryOut.peekFirst();

            if (first == null || first.time() >= threshold) {
                break;
            }

            byteHistoryOut.pollFirst();
        }
    }

    public void callHandlers(String mode, SocketAddress address, Object buffer, Long bytes) {
        if (!active.get()) return;
        List<TriConsumer<SocketAddress, Object, Long>> modeHandlers = handlers.get(mode);

        if (modeHandlers == null) return;

        modeHandlers.forEach(m -> m.accept(address, buffer, bytes));
    }

    public void registerStopMonitoringHandler(Consumer<Object[]> handler) {
        this.stopMonitoringHandler = handler;
    }

    public final void stopMonitoring(Object... args) {
        if (!active.compareAndSet(true, false)) return;
        handlers.clear();
        monitoringDuration = TimeUtils.currentSeconds() - timestamp;

        TrafficMonitorManager.instance().removeTrafficMonitor(this);

        Consumer<Object[]> handler = stopMonitoringHandler;
        if (!onStopMonitoring(args) && handler != null) {
            handler.accept(args);
        }
    }

    public boolean onStopMonitoring(Object... args) {
        return false;
    }

    public long getMonitoringDuration() {
        Long duration = monitoringDuration;

        if (duration != null) {
            return duration;
        }

        return TimeUtils.currentSeconds() - timestamp;
    }

    public long getTotalBytes() {
        return totalBytesOut.get() + totalBytesIn.get();
    }

    public long getAverageTotalBytes() {
        return getAverageBytesOut() + getAverageBytesIn();
    }

    public long getAverageBytesOut() {
        return byteHistoryOut.stream()
                .mapToLong(TrafficMonitorManager.TrafficData::bytes)
                .sum();
    }

    public long getAverageBytesIn() {
        return byteHistoryIn.stream()
                .mapToLong(TrafficMonitorManager.TrafficData::bytes)
                .sum();
    }
}