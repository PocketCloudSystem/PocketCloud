package de.pocketcloud.cloud.server;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.provider.write.IWriteServerProvider;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.util.CloudServerStorage;
import de.pocketcloud.cloud.server.util.ServerUtils;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.shared.component.data.CloudServerData;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

@Getter
@Accessors(fluent = true)
public final class CloudServerManager implements Tickable, IWriteServerProvider {

    public static final ExecutorService SERVER_EXECUTOR = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() / 4),
            r -> {
                Thread t = new Thread(r, "Server-Worker");
                t.setDaemon(false);
                return t;
            }
    );

    private static final int MAX_PARALLEL_PREPARES = 2;
    private static final int MAX_PARALLEL_STARTS = 2;
    private static final long START_STALL_THRESHOLD_MS = 5000;

    @Getter(AccessLevel.NONE)
    private final Map<String, Long> startingServerTimestamps = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Map<String, Long> preparingServerTimestamps = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Map<String, CloudServer> servers = new ConcurrentHashMap<>();
    @Getter(AccessLevel.NONE)
    private final Map<String, Integer> serverCounts = new ConcurrentHashMap<>();
    private final AtomicInteger startingServers = new AtomicInteger(0);
    private long tryAgainAt = 0;

    private boolean serverStartingEnabled = true;
    private long lastServerStartTime = 0;
    private long lastServerStopTime = 0;

    @Getter(AccessLevel.NONE)
    private final Map<String, String> latestServerStartTimes = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Map<String, Long> zeroPlayersReachedTimestamps = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final AtomicLong queueSequence = new AtomicLong(0);

    private final Comparator<CloudServer> QUEUE_ORDER = Comparator
            .comparingInt((CloudServer s) -> s.template().settings().priority())
            .reversed()
            .thenComparingLong(CloudServer::queuedAt);

    @Getter(AccessLevel.NONE)
    private final Queue<CloudServer> serverPrepareQueue = new PriorityBlockingQueue<>(11, QUEUE_ORDER);
    @Getter(AccessLevel.NONE)
    private final Queue<CloudServer> serverStartQueue = new PriorityBlockingQueue<>(11, QUEUE_ORDER);

    public CloudServerManager enableServerStarting() {
        serverStartingEnabled = true;
        return this;
    }

    public CloudServerManager disableServerStarting() {
        serverStartingEnabled = false;
        return this;
    }

    @Override
    public void add(ICloudServer server) {
        CloudServer cloudServer = requireCloudServer(server);
        servers.putIfAbsent(cloudServer.name(), cloudServer);
        ServerUtils.addId(cloudServer.template(), cloudServer.id());
    }

    @Override
    public void remove(ICloudServer server) {
        CloudServer cloudServer = requireCloudServer(server);
        if (!servers.remove(cloudServer.name(), cloudServer)) return;
        zeroPlayersReachedTimestamps.remove(cloudServer.name());
        serverCounts.compute(cloudServer.templateName(), (_, count) -> count == null ? 0 : Math.max(0, count - 1));
        if (startingServerTimestamps.remove(cloudServer.name()) != null) startingServers.decrementAndGet();
        preparingServerTimestamps.remove(cloudServer.name());
        serverPrepareQueue.remove(cloudServer);
        serverStartQueue.remove(cloudServer);
        ServerUtils.removeId(cloudServer.template(), cloudServer.id());
        ServerUtils.removePort(cloudServer.data().port());
        lastServerStopTime = System.currentTimeMillis();
        cloudServer.markForRemoval().syncOut();
    }

    @Override
    public Promise<Collection<String>> start(ITemplate template, int count) {
        Collection<String> startedServers = new ArrayList<>();
        int maxCount = template.settings().maxServerCount();

        int[] toStartHolder = new int[1];
        serverCounts.compute(template.name(), (t, current) -> {
            int existing = current == null ? 0 : current;
            int allowed = Math.clamp(maxCount - existing, 0, count);
            toStartHolder[0] = allowed;
            return existing + allowed;
        });

        int toStart = toStartHolder[0];

        if (toStart <= 0) {
            CloudLogger.get().warn("Failed to start any more servers of §b{} §rdue to the max amount of servers already being reached.", template.name());
            return Promise.resolved(startedServers);
        }

        for (int i = 0; i < toStart; i++) {
            this.lastServerStartTime = System.currentTimeMillis();

            int id = ServerUtils.reserveId(template);
            if (id == -1) {
                serverCounts.merge(template.name(), -1, Integer::sum);
                continue;
            }

            int port = ServerUtils.reservePort(template.templateType());
            if (port <= 0) {
                CloudLogger.get().warn("Failed to start any more servers of §b{}§8: §cNo available ports found.", template.name());
                serverCounts.merge(template.name(), -(toStart - i), Integer::sum);
                break;
            }

            UUID uuid = UUID.randomUUID();
            CloudServer server = new CloudServer(
                    id,
                    uuid,
                    template.name(),
                    new CloudServerData(uuid, template.templateType().isServer() ? "127.0.0.1" : "0.0.0.0", port, template.settings().maxPlayerCount()),
                    new CloudServerStorage(uuid)
            );

            latestServerStartTimes.put(template.name(), server.name());
            add(server);
            server.queuedAt(queueSequence.incrementAndGet());
            serverPrepareQueue.offer(server);
            startedServers.add(server.name());
        }

        return Promise.resolved(startedServers);
    }

    @Override
    public Promise<Void> save(ICloudServer server) {
        CloudServer cloudServer = requireCloudServer(server);
        String saveCommandLine = cloudServer.template().serverSoftware().config().saveCommandLine();
        if (saveCommandLine == null) {
            return cloudServer.save();
        }

        Promise<Void> promise = new Promise<>();
        cloudServer.dispatch(saveCommandLine)
                .thenSuccess(_ -> {
                    cloudServer.save().thenSuccess(_ -> promise.resolve(null))
                                    .failure(promise::reject);
                })
                .failure(promise::reject);

        return promise;
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(ICloudServer server, boolean force) {
        Collection<ICloudServer> affectedServers = new ArrayList<>(Collections.singleton(requireCloudServer(server)));
        affectedServers.forEach(s -> ((CloudServer) s).stop(force));
        return Promise.resolved(affectedServers);
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(ITemplate template, boolean force) {
        Collection<ICloudServer> affectedServers = query(ServerSearchQuery.create().ofTemplate(template));
        affectedServers.forEach(s -> ((CloudServer) s).stop(force));
        return Promise.resolved(affectedServers);
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(IServerGroup group, boolean force) {
        Collection<ICloudServer> affectedServers = query(ServerSearchQuery.create().inGroup(group));
        affectedServers.forEach(s -> ((CloudServer) s).stop(force));
        return Promise.resolved(affectedServers);
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(TemplateType templateType, boolean force) {
        Collection<ICloudServer> affectedServers = query(ServerSearchQuery.create().ofType(templateType));
        affectedServers.forEach(s -> ((CloudServer) s).stop(force));
        return Promise.resolved(affectedServers);
    }

    @Override
    public Promise<Collection<ICloudServer>> stop(String name, boolean force) {
        Collection<ICloudServer> affectedServers = new ArrayList<>();
        if (servers.containsKey(name)) affectedServers.add(servers.get(name));
        affectedServers.forEach(s -> ((CloudServer) s).stop(force));
        return Promise.resolved(affectedServers);
    }

    @Override
    public Promise<Collection<ICloudServer>> stopAll(boolean force) {
        Collection<ICloudServer> all = getAll();
        all.forEach(s -> ((CloudServer) s).stop(force));
        return Promise.resolved(all);
    }

    public Promise<Void> stopTemplateAndWait(ITemplate template, int timeoutMs) {
        return Promise.supplyAsync(() -> {
            Collection<ICloudServer> affectedServers;

            try {
                affectedServers = stop(template).get();
            } catch (Throwable e) {
                throw new RuntimeException("Failed to stop template " + template, e);
            }

            long deadline = System.nanoTime() + timeoutMs * 1_000_000L;
            while (System.nanoTime() < deadline) {
                boolean hasRunningServers = false;

                for (ICloudServer server : affectedServers) {
                    if (servers.containsKey(server.name())) {
                        hasRunningServers = true;
                        break;
                    }
                }

                if (!hasRunningServers) return null;
                LockSupport.parkNanos(1_000_000L);
            }

            for (ICloudServer server : affectedServers) {
                if (servers.containsKey(server.name())) {
                    stop(server, true);
                }
            }

            return null;
        });
    }

    public void stopAllAndWait(int timeoutMs) {
        stopAll();
        long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < timeoutMs) {
            if (servers.isEmpty()) break;
            LockSupport.parkNanos(1_000_000);
        }

        if (!servers.isEmpty()) {
            CloudLogger.get().warn("Some servers still running after trying to shut them down, force stopping...");
            stopAll(true);
        }
    }

    @Override
    public boolean check(String name) {
        return servers.containsKey(name);
    }

    @Override
    public boolean check(UUID uuid) {
        return servers.values().stream().anyMatch(s -> s.uuid().equals(uuid));
    }

    @Override
    public boolean checkCapacity(ITemplate template) {
        return serverCounts.getOrDefault(template, 0) < template.settings().maxServerCount();
    }

    private void addToStartQueue(CloudServer server) {
        CloudLogger.get().debug("Done preparing server: §b{}", server.name());
        server.queuedAt(queueSequence.incrementAndGet());
        serverStartQueue.offer(server);
    }

    private void onStartFailed(Object[] crashData) {
        CloudServer server = (CloudServer) crashData[0];
        Throwable exception = crashData.length > 1 ? (Throwable) crashData[1] : null;
        server.status(ServerStatus.OFFLINE);

        CloudLogger.get().warn(
                "§cFailed to prepare server §e{}§8: §e{}",
                server.name(),
                exception != null ? exception.getMessage() : "Unknown error"
        );

        if (exception != null) CloudLogger.get().exception(exception);
        if (startingServerTimestamps.remove(server.name()) != null) startingServers.decrementAndGet();
        ServerUtils.removePort(server.data().port());
        ServerUtils.removeId(server.template(), server.id());
    }

    @Override
    public void tick(long currentTick) {
        Map<String, Integer> remaining = new HashMap<>();
        for (CloudServer s : servers.values()) {
            ServerStatus st = s.status();
            if (!st.isStopping() && !st.isOffline()) remaining.merge(s.template().name(), 1, Integer::sum);
        }

        servers.values().forEach(server -> {
            if (server.status().isOnline()) {
                Template template = server.template();
                int playerCount = server.playerCount();
                long zeroPlayersReachedTimestamp = zeroPlayersReachedTimestamps.getOrDefault(server.name(), 0L);
                if (playerCount > 0) zeroPlayersReachedTimestamps.put(server.name(), 0L);
                if (playerCount == 0 && template.settings().stopOnEmpty()) {
                    if (zeroPlayersReachedTimestamp == 0) {
                        zeroPlayersReachedTimestamp = System.currentTimeMillis();
                        zeroPlayersReachedTimestamps.put(server.name(), zeroPlayersReachedTimestamp);
                    }

                    boolean graceOver = (zeroPlayersReachedTimestamp + (template.settings().emptyServerGracePeriod() * 1000L)) <= System.currentTimeMillis();
                    if (graceOver && remaining.getOrDefault(template.name(), 0) > template.settings().minServerCount()) {
                        remaining.merge(template.name(), -1, Integer::sum);
                        stop(server);
                        return;
                    }
                }
            }

            server.tick(currentTick);
        });

        if ((serverPrepareQueue.isEmpty() && serverStartQueue.isEmpty()) || currentTick < tryAgainAt) return;
        double cpuUsage = PocketCloud.instance().performanceStats().systemCpuUsage();
        if (cpuUsage >= 85) {
            CloudLogger.get().warn("Unable to process more server starts due to §chigh CPU load §8(§c{}%§8)§r.", cpuUsage);
            CloudLogger.get().warn("Trying again in 2 seconds. Pending Servers§8: §e{} §8| §a{}", serverPrepareQueue.size(), serverStartQueue.size());
            tryAgainAt = currentTick + 40;
            return;
        }

        Benchmark.startTiming("check_server_prepare_queue");

        int prepareSlots = MAX_PARALLEL_PREPARES - activePreparingSlots();
        while (prepareSlots > 0 && !serverPrepareQueue.isEmpty()) {
            CloudServer server = serverPrepareQueue.poll();
            if (server == null) break;

            preparingServerTimestamps.put(server.name(), System.currentTimeMillis());
            server.prepare()
                    .thenSuccess(_ -> {
                        preparingServerTimestamps.remove(server.name());
                        addToStartQueue(server);
                    })
                    .failure(e -> {
                        preparingServerTimestamps.remove(server.name());
                        onStartFailed(new Object[]{server, e});
                    });

            prepareSlots--;
        }

        Benchmark.stopTiming("check_server_prepare_queue");
        if (!serverStartingEnabled) return;
        Benchmark.startTiming("check_server_start_queue");

        int availableSlots = MAX_PARALLEL_STARTS - activeStartingSlots();
        while (availableSlots > 0 && !serverStartQueue.isEmpty()) {
            CloudServer server = serverStartQueue.poll();
            if (server == null) break;
            startingServerTimestamps.put(server.name(), System.currentTimeMillis());
            startingServers.incrementAndGet();
            server.start().boot();
            availableSlots--;
        }

        Benchmark.stopTiming("check_server_start_queue");
    }

    private int activePreparingSlots() {
        long now = System.currentTimeMillis();
        return (int) preparingServerTimestamps.values().stream()
                .filter(startedAt -> now - startedAt < START_STALL_THRESHOLD_MS)
                .count();
    }

    private int activeStartingSlots() {
        long now = System.currentTimeMillis();
        return (int) startingServerTimestamps.values().stream()
                .filter(startedAt -> now - startedAt < START_STALL_THRESHOLD_MS)
                .count();
    }

    public void handleHandshakeReceived(String serverName) {
        if (startingServerTimestamps.remove(serverName) != null) {
            startingServers.decrementAndGet();
        }
    }

    @Override
    public Optional<ICloudServer> get(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(servers.get(name));
    }

    @Override
    public Optional<ICloudServer> get(UUID uuid) {
        return widen(servers.values()).stream().filter(s -> s.uuid().compareTo(uuid) == 0).findFirst();
    }

    @Override
    public ICloudServer current() {
        throw new RuntimeException("There is no \"current\" server on the cloud side");
    }

    public Optional<ICloudServer> getFreeLobby() {
        return widen(servers.values()).stream()
                .filter(s -> s.template().settings().lobby() && s.playerCount() < s.data().maxPlayers())
                .min(Comparator.comparingInt(ICloudServer::playerCount));
    }

    public Optional<ICloudServer> getLatest(ITemplate template) {
        String latestName = latestServerStartTimes.get(template.name());
        if (latestName == null) return Optional.empty();
        return Optional.ofNullable(servers.get(latestName));
    }

    @Override
    public Collection<ICloudServer> query(ServerSearchQuery searchQuery) {
        return widen(servers.values().stream()
                .filter(searchQuery::matches)
                .toList());
    }

    @Override
    public Collection<ICloudServer> query(Consumer<ServerSearchQuery> queryConsumer) {
        ServerSearchQuery searchQuery = new ServerSearchQuery();
        queryConsumer.accept(searchQuery);
        return query(searchQuery);
    }

    @Override
    public int serverCount() {
        return servers.size();
    }

    @Override
    public Collection<ICloudServer> getAll() {
        return widen(List.copyOf(servers.values()));
    }

    @SuppressWarnings("unchecked")
    private <T extends ICloudServer> Collection<ICloudServer> widen(Collection<T> collection) {
        return (Collection<ICloudServer>) collection;
    }

    private CloudServer requireCloudServer(ICloudServer server) {
        if (!(server instanceof CloudServer cloudServer)) {
            throw new IllegalArgumentException("Unsupported ICloudServer implementation: " + server.getClass().getName());
        }

        return cloudServer;
    }
}