package de.pocketcloud.cloud.server;

import de.pocketcloud.api.model.group.IServerGroup;
import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.model.template.ITemplate;
import de.pocketcloud.api.search.SearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.api.provider.IServerProvider;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.packet.impl.ServerSyncPacket;
import de.pocketcloud.cloud.server.util.CloudServerData;
import de.pocketcloud.cloud.server.start.ServerStartMethod;
import de.pocketcloud.cloud.server.util.ServerStartMethods;
import de.pocketcloud.cloud.server.util.ServerUtils;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.common.concurrent.Promise;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@Accessors(fluent = true)
public final class CloudServerManager implements Tickable, IServerProvider<CloudServer> {

    public static final ExecutorService SERVER_EXECUTOR = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "Server-Worker");
                t.setDaemon(false);
                return t;
            }
    );

    private static final int MULTI_START_BATCH_SIZE = 5;
    private static final int MULTI_START_THRESHOLD = 5;

    private static final int MAX_PARALLEL_STARTS = 2;

    @Getter(AccessLevel.NONE)
    private final Map<String, CloudServer> servers = new ConcurrentHashMap<>();
    private final AtomicInteger startingServers = new AtomicInteger(0);

    private long lastServerStartTime = 0;
    private long lastServerStopTime = 0;
    private long nextServerStartTime = 0;

    private final Map<String, String> latestServerStartTimes = new ConcurrentHashMap<>();

    @Getter(AccessLevel.NONE)
    private final Queue<CloudServer> serverPrepareQueue = new LinkedList<>();
    @Getter(AccessLevel.NONE)
    private final Queue<CloudServer> serverStartQueue = new LinkedList<>();

    public void add(CloudServer server) {
        servers.putIfAbsent(server.name(), server);
        ServerUtils.addId(server.template(), server.id());
        ServerUtils.addPort(server.data().port());
    }

    public void remove(CloudServer server) {
        servers.remove(server.name());
        ServerUtils.removeId(server.template(), server.id());
        ServerUtils.removePort(server.data().port());
        this.lastServerStopTime = System.currentTimeMillis();
        ServerSyncPacket.create(server, true).broadcastPacket();
    }
    
    public Promise<Collection<String>> start(ITemplate template, int count) {
        Collection<String> startedServers = new ArrayList<>();

        if (!checkCapacity(template)) {
            CloudLogger.get().warn("Failed to start any more servers of §b{} §rdue to the max amount of servers already being reached.", template.name());
            return Promise.resolved(startedServers);
        }

        for (int i = 0; i < count; i++) {
            if (!checkCapacity(template)) break;
            this.lastServerStartTime = System.currentTimeMillis();

            int id = ServerUtils.getFreeId(template);
            if (id == -1) continue;

            int port = ServerUtils.getFreePort(template.templateType());
            if (port <= 0) {
                CloudLogger.get().warn("Failed to start any more servers of §b{}§8: §cNo available ports found.", template.name());
                break;
            }

            UUID uuid = UUID.randomUUID();
            CloudServer server = new CloudServer(
                id,
                uuid,
                template.name(),
                new CloudServerData(uuid, port, template.settings().maxPlayerCount())
            );

            latestServerStartTimes.put(template.name(), server.name());
            add(server);
            serverPrepareQueue.offer(server);
            startedServers.add(server.name());
        }

        return Promise.resolved(startedServers);
    }

    public Promise<Void> save(CloudServer server) {
        String saveCommandLine = server.template().serverSoftware().config().saveCommandLine();
        if (saveCommandLine == null) {
            return server.save();
        }

        Promise<Void> promise = new Promise<>();
        server.dispatch(saveCommandLine)
            .thenSuccess(_ -> {
                server.save();
                promise.resolve(null);
            })
            .failure(promise::reject);

        return promise;
    }

    public Promise<Collection<CloudServer>> stop(CloudServer server, boolean force) {
        Collection<CloudServer> affectedServers = new ArrayList<>(Collections.singleton(server));
        affectedServers.forEach(s -> s.stop(force));
        return Promise.resolved(affectedServers);
    }

    public Promise<Collection<CloudServer>> stop(ITemplate template, boolean force) {
        Collection<CloudServer> affectedServers = query(ServerSearchQuery.create().ofTemplate(template));
        affectedServers.forEach(server -> server.stop(force));
        return Promise.resolved(affectedServers);
    }

    public Promise<Collection<CloudServer>> stop(IServerGroup group, boolean force) {
        Collection<CloudServer> affectedServers = query(ServerSearchQuery.create().inGroup(group));
        affectedServers.forEach(server -> server.stop(force));
        return Promise.resolved(affectedServers);
    }

    public Promise<Collection<CloudServer>> stop(TemplateType templateType, boolean force) {
        Collection<CloudServer> affectedServers = query(ServerSearchQuery.create().ofType(templateType));
        affectedServers.forEach(server -> server.stop(force));
        return Promise.resolved(affectedServers);
    }

    public Promise<Collection<CloudServer>> stop(String name, boolean force) {
        Collection<CloudServer> affectedServers = new ArrayList<>();
        if (servers.containsKey(name)) affectedServers.add(servers.get(name));
        affectedServers.forEach(server -> server.stop(force));
        return Promise.resolved(affectedServers);
    }
    
    public Promise<Collection<CloudServer>> stopAll(boolean force) {
        Collection<CloudServer> all = getAll();
        all.forEach(server -> server.stop(force));
        return Promise.resolved(all);
    }

    @Override
    public boolean check(String name) {
        return false;
    }

    @Override
    public boolean check(UUID uuid) {
        return false;
    }

    public boolean checkCapacity(ITemplate template) {
        return query(ServerSearchQuery.create().ofTemplate(template)).size() < template.settings().maxServerCount();
    }

    private void addToStartQueue(CloudServer server) {
        CloudLogger.get().debug("Done preparing server: §b{}", server.name());
        serverStartQueue.offer(server);
    }

    private void onStartFailed(Object[] crashData) {
        CloudServer server = (CloudServer) crashData[0];
        Throwable exception = crashData.length > 1 ? (Throwable) crashData[1] : null;
        CloudLogger.get().warn("§cFailed to prepare server §e{}§8: §e{}", server.name(), exception != null ? exception.getMessage() : "Unknown error");
        if (exception != null) CloudLogger.get().exception(exception);
    }

    @Override
    public void tick(long currentTick) {
        servers.values().forEach(server -> server.tick(currentTick));


        /**
         * TODO
         * <pr>How the cloud should actually start servers to save CPU</pr>
         * {constant MAX_PARALLEL_STARTS = 2;}
         * The cloud starts 2 servers and waits until they connected to the cloud + sent the respective HandshakePacket.
         * The amount of servers currently starting is being saved to an AtomicInteger - which when reaching anything below 2, the cloud starts a new amount of servers by this formula:
         * MAX_PARALLEL_STARTS - AtomicInteger.get() = n
         *
         * If a boot sequence of a server takes too long (not the server timeout specified inside the ServerSoftware) by a fixed amount of seconds (most likely 5), the cloud will just
         * start another server.
         *
         */

        if (!serverPrepareQueue.isEmpty()) {
            Benchmark.startTiming("check_server_prepare_queue");

            CloudServer server = serverPrepareQueue.poll();
            server.prepare()
                .thenSuccess(_ -> addToStartQueue(server))
                .failure(e -> onStartFailed(new Object[]{server, e}));

            Benchmark.stopTiming("check_server_prepare_queue");
            return;
        }

        Benchmark.startTiming("check_server_start_queue");
        if (currentTick >= nextServerStartTime && !serverStartQueue.isEmpty()) {
            ServerStartMethod method = ServerStartMethods.current();
            int queueSize = serverStartQueue.size();
            if (queueSize >= MULTI_START_THRESHOLD) {
                Collection<CloudServer> batch = new ArrayList<>();
                int limit = Math.min(queueSize, MULTI_START_BATCH_SIZE);
                for (int i = 0; i < limit; i++) batch.add(serverStartQueue.poll());

                batch.forEach(CloudServer::start);
                method.start(batch.toArray(new CloudServer[0])).thenSuccess(map -> {
                    for (CloudServer server : batch) {
                        if (map.containsKey(server.name())) {
                            CloudServersHandler.handleStartSuccess(server, map.get(server.name()));
                        } else {
                            CloudServersHandler.handleStartFailure(server, null, true);
                        }
                    }
                }).failure(ex -> {
                    CloudLogger.get().exception("Failed to start servers", ex);
                    for (CloudServer server : batch) {
                        CloudServersHandler.handleStartFailure(server, ex, true);
                    }
                });
            } else {
                Objects.requireNonNull(serverStartQueue.poll()).start().boot();
            }

            nextServerStartTime = currentTick + 10;
        }

        Benchmark.stopTiming("check_server_start_queue");
    }

    public Optional<CloudServer> get(String name) {
        return Optional.ofNullable(servers.getOrDefault(name,
            servers.values().stream()
                .filter(s -> s.name().startsWith(name))
                .findFirst().orElse(null)
        ));
    }

    public Optional<CloudServer> get(UUID uuid) {
        return servers.values().stream().filter(s -> s.uuid().compareTo(uuid) == 0).findFirst();
    }

    public Optional<CloudServer> getFreeLobby() {
        return servers.values().stream()
                .filter(s -> s.template().settings().lobby() && s.playerCount() < s.data().maxPlayers())
                .min(Comparator.comparingInt(CloudServer::playerCount));
    }

    public Optional<CloudServer> getLatest(Template template) {
        String latestName = latestServerStartTimes.get(template.name());
        if (latestName == null) return Optional.empty();
        return Optional.ofNullable(servers.getOrDefault(latestName, null));
    }

    @Override
    public Collection<CloudServer> query(SearchQuery<? extends ICloudServer> searchQuery) {
        return filter(searchQuery);
    }

    @SuppressWarnings("unchecked")
    private <T extends ICloudServer> Collection<CloudServer> filter(SearchQuery<T> searchQuery) {
        return servers.values().stream()
                .filter(o -> searchQuery.matches((T) o))
                .toList();
    }

    public Set<CloudServer> getAll() {
        return new HashSet<>(servers.values());
    }
}