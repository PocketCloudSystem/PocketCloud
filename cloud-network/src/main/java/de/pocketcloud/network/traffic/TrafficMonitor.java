package de.pocketcloud.network.traffic;

import de.pocketcloud.common.function.TriConsumer;
import de.pocketcloud.common.util.TimeUtils;
import io.netty.channel.Channel;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class TrafficMonitor {

    private static final double WINDOW_SECONDS = 1.0;

    @Getter
    protected final AtomicBoolean active = new AtomicBoolean(true);
    @Getter
    protected final long timestamp;
    protected volatile Long monitoringDuration = null;
    protected final Map<de.pocketcloud.api.network.traffic.TrafficDirection, List<TriConsumer<Channel, Object, Long>>> handlers = new ConcurrentHashMap<>();
    protected final Map<de.pocketcloud.api.network.traffic.TrafficDirection, TrafficWindow> windows = new ConcurrentHashMap<>();
    protected volatile Consumer<Object[]> stopMonitoringHandler = null;

    protected TrafficMonitor() {
        this.timestamp = TimeUtils.currentSeconds();

        for (de.pocketcloud.api.network.traffic.TrafficDirection direction : de.pocketcloud.api.network.traffic.TrafficDirection.values()) {
            windows.put(direction, new TrafficWindow(WINDOW_SECONDS));
            handlers.put(direction, new CopyOnWriteArrayList<>());
        }
    }

    public TrafficMonitor monitorIn(TriConsumer<Channel, Object, Long> handler) {
        return addHandler(de.pocketcloud.api.network.traffic.TrafficDirection.IN, handler);
    }

    public TrafficMonitor monitorOut(TriConsumer<Channel, Object, Long> handler) {
        return addHandler(de.pocketcloud.api.network.traffic.TrafficDirection.OUT, handler);
    }

    protected TrafficMonitor addHandler(de.pocketcloud.api.network.traffic.TrafficDirection direction, TriConsumer<Channel, Object, Long> handler) {
        if (!handlers.containsKey(direction)) handlers.put(direction, new CopyOnWriteArrayList<>());
        handlers.get(direction).add(handler);
        return this;
    }

    public void pushBytes(de.pocketcloud.api.network.traffic.TrafficDirection direction, long bytes) {
        if (!active.get()) return;
        windows.get(direction).push(bytes);
    }

    public void cleanupHistory() {
        windows.values().forEach(TrafficWindow::cleanup);
    }

    public void callHandlers(de.pocketcloud.api.network.traffic.TrafficDirection direction, Channel address, Object buffer, Long bytes) {
        if (!active.get()) return;
        if (!handlers.containsKey(direction)) return;
        handlers.get(direction).forEach(handler -> handler.accept(address, buffer, bytes));
    }

    public void registerStopMonitoringHandler(Consumer<Object[]> handler) {
        this.stopMonitoringHandler = handler;
    }

    public final void stopMonitoring(Object... args) {
        if (!active.compareAndSet(true, false)) return;
        clearHandlers();
        monitoringDuration = TimeUtils.currentSeconds() - timestamp;

        TrafficMonitorManager.instance().removeTrafficMonitor(this);

        Consumer<Object[]> handler = stopMonitoringHandler;
        if (!onStopMonitoring(args) && handler != null) {
            handler.accept(args);
        }
    }

    protected void clearHandlers() {
        handlers.values().forEach(List::clear);
    }

    public boolean onStopMonitoring(Object... args) {
        return false;
    }

    public long getMonitoringDuration() {
        Long duration = monitoringDuration;
        if (duration != null) return duration;
        return TimeUtils.currentSeconds() - timestamp;
    }

    public long getTotalBytes() {
        return getTotalBytes(de.pocketcloud.api.network.traffic.TrafficDirection.IN) + getTotalBytes(de.pocketcloud.api.network.traffic.TrafficDirection.OUT);
    }

    public long getTotalBytes(de.pocketcloud.api.network.traffic.TrafficDirection direction) {
        return windows.get(direction).total();
    }

    public long getAverageTotalBytes() {
        return getAverageBytes(de.pocketcloud.api.network.traffic.TrafficDirection.IN) + getAverageBytes(de.pocketcloud.api.network.traffic.TrafficDirection.OUT);
    }

    public long getAverageBytes(de.pocketcloud.api.network.traffic.TrafficDirection direction) {
        return windows.get(direction).windowSum();
    }

    public static String getName() {
        return null;
    }
}