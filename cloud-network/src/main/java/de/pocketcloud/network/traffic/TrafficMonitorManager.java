package de.pocketcloud.network.traffic;

import de.pocketcloud.api.network.traffic.TrafficDirection;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.network.traffic.impl.NetworkTrafficMonitor;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Getter
@Accessors(fluent = true)
public final class TrafficMonitorManager implements Tickable, Loadable {

    @Getter
    @Accessors(fluent = true)
    private static TrafficMonitorManager instance = null;

    private final Map<Class<? extends TrafficMonitor>, List<TrafficMonitor>> trafficMonitors = new ConcurrentHashMap<>();
    private final Map<Class<? extends TrafficMonitor>, String> trafficMonitorNames = new ConcurrentHashMap<>();
    private final Map<Class<? extends TrafficMonitor>, Map<TrafficDirection, TrafficWindow>> globalWindows = new ConcurrentHashMap<>();

    public TrafficMonitorManager() {
        instance = this;
    }

    @Override
    public void load() {
        registerTrafficMonitorType(NetworkTrafficMonitor.class, "network");
    }

    @Override
    public void unload() {
        trafficMonitors.clear();
    }

    public void registerTrafficMonitorType(Class<? extends TrafficMonitor> type, String name) {
        if (globalWindows.containsKey(type)) return;
        Map<TrafficDirection, TrafficWindow> windows = new ConcurrentHashMap<>();
        for (TrafficDirection direction : TrafficDirection.values()) {
            windows.put(direction, new TrafficWindow(1.0));
        }

        trafficMonitorNames.put(type, name);
        globalWindows.put(type, windows);
    }

    public void unregisterTrafficMonitorType(Class<? extends TrafficMonitor> type) {
        if (globalWindows.containsKey(type)) {
            globalWindows.remove(type);
            trafficMonitors.remove(type);
            trafficMonitorNames.remove(type);
        }
    }

    @Override
    public void tick(long currentTick) {
        if (currentTick % 20 != 0) return;
        globalWindows.values().forEach(windows -> windows.values().forEach(TrafficWindow::cleanup));
        trafficMonitors.values().forEach(monitors -> monitors.forEach(TrafficMonitor::cleanupHistory));
    }

    @SuppressWarnings("unchecked")
    public <T extends TrafficMonitor> T createTrafficMonitor(Class<T> type) {
        if (!globalWindows.containsKey(type))
            throw new IllegalArgumentException("Unknown TrafficMonitor type: " + type.getName());
        TrafficMonitor monitor;
        try {
            monitor = ((Class<? extends TrafficMonitor>) type).getDeclaredConstructor().newInstance();
            trafficMonitors.computeIfAbsent(type, _ -> new CopyOnWriteArrayList<>()).add(monitor);
            return (T) monitor;
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeTrafficMonitor(TrafficMonitor monitor) {
        List<TrafficMonitor> monitors = trafficMonitors.get(monitor.getClass());
        if (monitors != null) monitors.remove(monitor);
    }

    public void pushBytes(Class<? extends TrafficMonitor> type, TrafficDirection direction, long bytes) {
        Map<TrafficDirection, TrafficWindow> windows = globalWindows.get(type);
        if (windows == null) return;
        windows.get(direction).push(bytes);

        List<TrafficMonitor> monitors = trafficMonitors.get(type);
        if (monitors != null) {
            for (TrafficMonitor monitor : monitors) {
                monitor.pushBytes(direction, bytes);
            }
        }
    }

    public void callHandlers(Class<? extends TrafficMonitor> type, TrafficDirection direction, Channel channel, Object buffer, Long bytes) {
        List<TrafficMonitor> monitors = trafficMonitors.get(type);
        if (monitors == null) return;
        for (TrafficMonitor monitor : monitors) {
            monitor.callHandlers(direction, channel, buffer, bytes);
        }
    }

    public List<TrafficMonitor> trafficMonitors(Class<? extends TrafficMonitor> type) {
        return trafficMonitors.get(type);
    }

    public long totalBytes(Class<? extends TrafficMonitor> type, TrafficDirection direction) {
        Map<TrafficDirection, TrafficWindow> windows = globalWindows.get(type);
        return windows == null ? 0L : windows.get(direction).total();
    }

    public long totalBytesAll(TrafficDirection direction) {
        return globalWindows.values().stream()
                .map(windows -> windows.get(direction))
                .filter(Objects::nonNull)
                .mapToLong(TrafficWindow::total)
                .sum();
    }

    public long averageBytes(Class<? extends TrafficMonitor> type, TrafficDirection direction) {
        Map<TrafficDirection, TrafficWindow> windows = globalWindows.get(type);
        return windows == null ? 0L : windows.get(direction).windowSum();
    }

    public long averageBytesAll(TrafficDirection direction) {
        return globalWindows.values().stream()
                .map(windows -> windows.get(direction))
                .filter(Objects::nonNull)
                .mapToLong(TrafficWindow::windowSum)
                .sum();
    }

    public String name(Class<? extends TrafficMonitor> type) {
        return trafficMonitorNames.getOrDefault(type, "unknown");
    }
}