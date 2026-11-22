package de.pocketcloud.cloud.server;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.provider.write.IWriteServerProvider;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.NodeHandle;
import de.pocketcloud.cloud.cluster.ServerAddresses;
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
        cloudServer.node().releasePort(cloudServer.data().port());
        lastServerStopTime = System.currentTimeMillis();
        cloudServer.markForRemoval().syncOut();
        PocketCloud.instance().players().handleServerRemoved(cloudServer);
    }

    @Override
    public Promise<Collection<String>> start(ITemplate template, int count) {
        ClusterManager cluster = PocketCloud.instance().cluster();
        if (!cluster.isHead()) return cluster.forwardStart(template.name(), count);

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

            NodeHandle node = cluster.selectNodeForNewServer(template);
            if (node == null) {
                // No node may run this template right now (allowed nodes, memory, CPU): give the id back.
                ServerUtils.removeId(template, id);
                serverCounts.merge(template.name(), -(toStart - i), Integer::sum);
                break;
            }

            if (!node.local()) {
                startOnRemoteNode(template, id, node, false);
                latestServerStartTimes.put(template.name(), template.name() + "-" + id);
                startedServers.add(template.name() + "-" + id);
                continue;
            }

            String name = queueLocalStart(template, id, node);
            if (name == null) {
                CloudLogger.get().warn("Failed to start any more servers of §b{}§8: §cNo available ports found.", template.name());
                ServerUtils.removeId(template, id);
                serverCounts.merge(template.name(), -(toStart - i), Integer::sum);
                break;
            }
            startedServers.add(name);
        }

        return Promise.resolved(startedServers);
    }

    /**
     * Head: creates a server of this node and puts it into the prepare queue.
     *
     * @return the server name, or null if no port is free
     */
    private String queueLocalStart(ITemplate template, int id, NodeHandle node) {
        int port = node.reservePort(template.templateType());
        if (port <= 0) return null;

        UUID uuid = UUID.randomUUID();
        CloudServer server = new CloudServer(
                id,
                uuid,
                template.name(),
                new CloudServerData(uuid, ServerAddresses.publicAddress(), port, template.settings().maxPlayerCount()),
                new CloudServerStorage(uuid)
        );

        latestServerStartTimes.put(template.name(), server.name());
        add(server);
        server.queuedAt(queueSequence.incrementAndGet());
        serverPrepareQueue.offer(server);
        return server.name();
    }

    /**
     * Head: a node could not start a server of the template (rejected, failed or no answer). Tries once more
     * on the next best node; gives up with a warning if there is none or that start fails too.
     */
    private void retryStart(ITemplate template, String failedNode) {
        ClusterManager cluster = PocketCloud.instance().cluster();
        if (!cluster.isHead()) return;
        boolean[] allowed = new boolean[1];
        serverCounts.compute(template.name(), (_, current) -> {
            int existing = current == null ? 0 : current;
            allowed[0] = existing < template.settings().maxServerCount();
            return allowed[0] ? existing + 1 : existing;
        });
        if (!allowed[0]) return;

        NodeHandle node = cluster.selectNodeForNewServer(template, Set.of(failedNode));
        int id = node == null ? -1 : ServerUtils.reserveId(template);
        if (node == null || id == -1) {
            serverCounts.merge(template.name(), -1, Integer::sum);
            CloudLogger.get().warn("§cGiving up §rthe start of a server of §b{}§r: it failed on §b{} §rand no other node can take it.", template.name(), failedNode);
            return;
        }

        CloudLogger.get().info("Retrying the start of a server of §b{} §ron node §b{} §8(§rfailed on §b{}§8)§r...", template.name(), node.name(), failedNode);
        if (!node.local()) {
            startOnRemoteNode(template, id, node, true);
            latestServerStartTimes.put(template.name(), template.name() + "-" + id);
        } else if (queueLocalStart(template, id, node) == null) {
            ServerUtils.removeId(template, id);
            serverCounts.merge(template.name(), -1, Integer::sum);
            CloudLogger.get().warn("§cGiving up §rthe start of a server of §b{}§r: no free port on §b{}§r.", template.name(), node.name());
        }
    }

    /**
     * Head side: registers a placeholder for the server and asks the node to start it.
     */
    private void startOnRemoteNode(ITemplate template, int id, NodeHandle node, boolean retry) {
        UUID uuid = UUID.randomUUID();
        CloudServer placeholder = new CloudServer(id, uuid, template.name(),
                new CloudServerData(uuid, "0.0.0.0", 0, template.settings().maxPlayerCount()),
                new CloudServerStorage(uuid));
        placeholder.nodeName(node.name());
        add(placeholder);
        CloudLogger.get().info("§aStarting §b{} §ron node §b{}§r...", placeholder.name(), node.name());

        PocketCloud.instance().cluster().requestRemoteStart(node.name(), template.name(), id, uuid)
                .failure(e -> {
                    CloudLogger.get().warn("Node §b{} §rcould not start §b{}§8: §c{}", node.name(), placeholder.name(), e.getMessage());
                    // without an answer the node may still start it later: cancel it there
                    PocketCloud.instance().cluster().cancelRemoteStart(node.name(), placeholder.name());
                    if (servers.get(placeholder.name()) != placeholder) return;
                    placeholder.status(ServerStatus.OFFLINE);
                    remove(placeholder);
                    if (retry) {
                        CloudLogger.get().warn("§cGiving up §rthe start of a server of §b{}§r: the retry on §b{} §rfailed too.", template.name(), node.name());
                    } else {
                        retryStart(template, node.name());
                    }
                });
    }

    /**
     * Node side: the head asked this node to start a server with a given id and uuid.
     *
     * @return the reserved port
     */
    public int startLocal(ITemplate template, int id, UUID uuid) {
        String name = template.name() + "-" + id;
        if (servers.containsKey(name)) throw new IllegalStateException("Server " + name + " already exists on this node");

        NodeHandle node = PocketCloud.instance().cluster().localNode();
        int port = node.reservePort(template.templateType());
        if (port <= 0) throw new IllegalStateException("No free port");

        CloudServer server = new CloudServer(
                id,
                uuid,
                template.name(),
                new CloudServerData(uuid, ServerAddresses.publicAddress(), port, template.settings().maxPlayerCount()),
                new CloudServerStorage(uuid)
        );

        serverCounts.merge(template.name(), 1, Integer::sum);
        latestServerStartTimes.put(template.name(), server.name());
        add(server);
        server.queuedAt(queueSequence.incrementAndGet());
        serverPrepareQueue.offer(server);
        server.syncOut();
        return port;
    }

    /**
     * Takes over a server process of the last run of this node (see {@code ServerRecovery}).
     *
     * @return false if a server with that name exists
     */
    public boolean adopt(CloudServer server) {
        if (servers.putIfAbsent(server.name(), server) != null) return false;
        ServerUtils.addId(server.template(), server.id());
        serverCounts.merge(server.templateName(), 1, Integer::sum);
        latestServerStartTimes.put(server.templateName(), server.name());
        return true;
    }

    /**
     * Applies the state of a server running on another node.
     */
    public void applyRemoteServer(CloudServer incoming) {
        // Templates may not be loaded yet (early full report) or not replicated to this node: the next full report adds the server.
        if (CloudAPI.instance().templates().get(incoming.templateName()).isEmpty()) {
            CloudLogger.get().debug("Ignoring server {} of node {}: template {} is unknown here.", incoming.name(), incoming.nodeName(), incoming.templateName());
            return;
        }

        CloudServer existing = servers.get(incoming.name());
        if (existing == null) {
            servers.put(incoming.name(), incoming);
            // Keep id and count bookkeeping cluster-wide, so a new head does not hand out the same id twice.
            ServerUtils.addId(incoming.template(), incoming.id());
            serverCounts.merge(incoming.templateName(), 1, Integer::sum);
            incoming.syncOut();
            if (incoming.status().isOnline()) incoming.addToProxies();
            return;
        }

        if (!existing.remote()) {
            if (existing.nodeName() == null || existing.nodeName().equals(incoming.nodeName())) return;
            CloudLogger.get().warn("Node §b{} §rreports a server §b{} §rthat also exists on this node. Ignoring it.", incoming.nodeName(), incoming.name());
            return;
        }

        // Replace the data object: address and port are only known once the node has reserved them.
        boolean wasOnline = existing.status().isOnline();
        servers.replace(incoming.name(), existing, incoming);
        incoming.syncOut();
        if (!wasOnline && incoming.status().isOnline()) incoming.addToProxies();
    }

    /**
     * Removes a server of another node (it stopped, or the node is gone).
     */
    public void removeRemoteServer(String name) {
        CloudServer existing = servers.get(name);
        if (existing == null || !existing.remote()) return;
        existing.status(ServerStatus.OFFLINE);
        existing.remove();
    }

    public Collection<CloudServer> serversOfNode(String nodeName) {
        return servers.values().stream().filter(s -> nodeName.equals(s.nodeName())).toList();
    }

    public Collection<CloudServer> localServers() {
        return servers.values().stream().filter(s -> !s.remote()).toList();
    }

    @Override
    public Promise<Void> save(ICloudServer server) {
        CloudServer cloudServer = requireCloudServer(server);
        if (cloudServer.remote()) return cloudServer.node().saveFiles(cloudServer); // the owning node runs the save command too
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

    /**
     * Stops the servers of this node (used on shutdown). Servers of other nodes keep running.
     */
    public void stopAllAndWait(int timeoutMs) {
        localServers().forEach(s -> s.stop(false));
        long start = System.currentTimeMillis();
        while ((System.currentTimeMillis() - start) < timeoutMs) {
            if (localServers().isEmpty()) break;
            LockSupport.parkNanos(1_000_000);
        }

        if (!localServers().isEmpty()) {
            CloudLogger.get().warn("Some servers still running after trying to shut them down, force stopping...");
            localServers().forEach(s -> s.stop(true));
        }

        // Servers of other nodes keep running there; this node simply stops tracking them.
        servers.values().removeIf(CloudServer::remote);
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
        server.node().releasePort(server.data().port());
        ServerUtils.removeId(server.template(), server.id());
    }

    @Override
    public void tick(long currentTick) {
        Map<String, Integer> remaining = new HashMap<>();
        for (CloudServer s : servers.values()) {
            ServerStatus st = s.status();
            if (!st.isStopping() && !st.isOffline()) remaining.merge(s.templateName(), 1, Integer::sum);
        }

        // Only the head decides about stopping empty servers (cluster-wide counts, no double stops).
        boolean scheduler = PocketCloud.instance().cluster().canSchedule();
        servers.values().forEach(server -> {
            if (scheduler && server.status().isOnline()) {
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