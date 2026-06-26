package de.pocketcloud.cloud.traffic;

import de.pocketcloud.cloud.load.Loadable;
import de.pocketcloud.cloud.tick.Tickable;
import de.pocketcloud.cloud.traffic.impl.NetworkTrafficMonitor;
import de.pocketcloud.cloud.util.TimeUtils;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.SocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

@Getter
@Accessors(fluent = true)
public final class TrafficMonitorManager implements Tickable, Loadable {

    public static final String TRAFFIC_NETWORK = "network";

    @Getter
    private static TrafficMonitorManager instance;

    private final Map<String, Supplier<? extends TrafficMonitor>> trafficMonitorTypes = new ConcurrentHashMap<>();
    private final Map<String, List<TrafficMonitor>> trafficMonitors = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Long>> allTimeTraffic = new ConcurrentHashMap<>();
    @Getter(AccessLevel.NONE)
    private final Map<String, Map<String, List<TrafficData>>> byteHistory = new ConcurrentHashMap<>();

    public TrafficMonitorManager() {
        instance = this;
    }

    @Override
    public void load() {
        registerTrafficMonitorType(TRAFFIC_NETWORK, NetworkTrafficMonitor::new);
    }

    @Override
    public void unload() {
        trafficMonitorTypes.clear();
        trafficMonitors.clear();
    }

    public void registerTrafficMonitorType(String type, Supplier<? extends TrafficMonitor> factory) {
        registerTrafficMonitorType(type, factory, false);
    }

    public void registerTrafficMonitorType(String type, Supplier<? extends TrafficMonitor> factory, boolean override) {
        if (trafficMonitorTypes.containsKey(type) && !override) return;

        trafficMonitorTypes.put(type, factory);

        Map<String, Long> allTime = new ConcurrentHashMap<>();
        allTime.put(TrafficMonitor.REGULAR_MODE_IN, 0L);
        allTime.put(TrafficMonitor.REGULAR_MODE_OUT, 0L);
        allTime.put(TrafficMonitor.REGULAR_MODE_IN + TrafficMonitor.SUFFIX_AVG, 0L);
        allTime.put(TrafficMonitor.REGULAR_MODE_OUT + TrafficMonitor.SUFFIX_AVG, 0L);
        allTimeTraffic.put(type, allTime);

        Map<String, List<TrafficData>> history = new ConcurrentHashMap<>();
        history.put(TrafficMonitor.REGULAR_MODE_IN, new CopyOnWriteArrayList<>());
        history.put(TrafficMonitor.REGULAR_MODE_OUT, new CopyOnWriteArrayList<>());
        byteHistory.put(type, history);
    }

    @Override
    public void tick(long currentTick) {
        if (currentTick % 20 != 0) return;
        cleanupHistory();
        trafficMonitors.values().forEach(monitors -> monitors.forEach(TrafficMonitor::cleanupHistory));
    }

    private void cleanupHistory() {
        double threshold = TimeUtils.currentTime() - 1.0;

        for (Map.Entry<String, Map<String, List<TrafficData>>> typeEntry : byteHistory.entrySet()) {
            String type = typeEntry.getKey();
            for (Map.Entry<String, List<TrafficData>> modeEntry : typeEntry.getValue().entrySet()) {
                String mode = modeEntry.getKey();
                List<TrafficData> history = modeEntry.getValue();

                history.removeIf(data -> data.time() < threshold);

                long avg = history.stream()
                        .mapToLong(TrafficData::bytes)
                        .sum();

                Map<String, Long> traffic = allTimeTraffic.get(type);
                if (traffic != null) {
                    traffic.put(mode + TrafficMonitor.SUFFIX_AVG, avg);
                }
            }
        }
    }

    public NetworkTrafficMonitor createNetworkMonitor() {
        TrafficMonitor monitor = createTrafficMonitor(TRAFFIC_NETWORK).orElse(null);
        if (!(monitor instanceof NetworkTrafficMonitor networkMonitor)) {
            throw new IllegalStateException("Registered monitor factory for traffic type " + TRAFFIC_NETWORK + " did not produce a NetworkTrafficMonitor");
        }

        return networkMonitor;
    }

    public Optional<TrafficMonitor> createTrafficMonitor(String type) {
        Supplier<? extends TrafficMonitor> supplier = trafficMonitorTypes.get(type);
        if (supplier == null) return Optional.empty();

        TrafficMonitor monitor = supplier.get();
        trafficMonitors.computeIfAbsent(type, _ -> new CopyOnWriteArrayList<>())
                .add(monitor);

        return Optional.of(monitor);
    }

    public void removeTrafficMonitor(TrafficMonitor monitor) {
        List<TrafficMonitor> monitors = trafficMonitors.get(monitor.getMonitorType());
        if (monitors != null) {
            monitors.remove(monitor);
        }
    }

    public void pushBytes(String type, long bytes, String mode) {
        Map<String, Long> typeTraffic = allTimeTraffic.get(type);
        if (typeTraffic == null || !typeTraffic.containsKey(mode)) return;
        typeTraffic.merge(mode, bytes, Long::sum);
        Map<String, List<TrafficData>> typeHistory = byteHistory.get(type);
        if (typeHistory != null) {
            List<TrafficData> history = typeHistory.computeIfAbsent(mode, _ -> new CopyOnWriteArrayList<>());
            history.add(new TrafficData(TimeUtils.currentTime(), bytes));
        }

        List<TrafficMonitor> monitors = trafficMonitors.get(type);
        if (monitors != null) {
            for (TrafficMonitor monitor : monitors) {
                monitor.pushBytes(mode, bytes);
            }
        }
    }

    public void callHandlers(String type, String mode, SocketAddress address, Object buffer, Long bytes) {
        List<TrafficMonitor> monitors = trafficMonitors.get(type);

        if (monitors != null) {
            for (TrafficMonitor monitor : monitors) {
                monitor.callHandlers(mode, address, buffer, bytes);
            }
        }
    }

    public List<TrafficMonitor> trafficMonitors(String type) {
        return trafficMonitors.get(type);
    }

    public Map<String, Long> allTimeTraffic(String type) {
        return allTimeTraffic.get(type);
    }

    public Optional<Long> allTimeTraffic(String type, String mode) {
        if (allTimeTraffic.containsKey(type)) return Optional.ofNullable(allTimeTraffic.get(type).getOrDefault(mode, null));
        return Optional.empty();
    }

    public record TrafficData(double time, long bytes) {}
}