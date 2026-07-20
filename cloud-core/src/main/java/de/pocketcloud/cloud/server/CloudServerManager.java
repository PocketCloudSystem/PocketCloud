package de.pocketcloud.cloud.server;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.provider.write.IWriteServerProvider;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.start.ServerStartMethod;
import de.pocketcloud.cloud.server.util.ServerStartMethods;
import de.pocketcloud.cloud.server.util.ServerUtils;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Getter
@Accessors(fluent = true)
public final class CloudServerManager implements Tickable, IWriteServerProvider {

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

    @Override
    public void add(ICloudServer server) {
        CloudServer cloudServer = requireCloudServer(server);
        servers.putIfAbsent(cloudServer.name(), cloudServer);
        ServerUtils.addId(cloudServer.template(), cloudServer.id());
        ServerUtils.addPort(cloudServer.data().port());
    }

    @Override
    public void remove(ICloudServer server) {
        servers.remove(server.name());
        ServerUtils.removeId(server.template(), server.id());
        ServerUtils.removePort(server.data().port());
        this.lastServerStopTime = System.currentTimeMillis();
        ((CloudServer) server).markForRemoval().syncOut();
    }

    @Override
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
                cloudServer.save();
                promise.resolve(null);
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
        servers.values().forEach(server -> ((CloudServer) server).tick(currentTick));


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

    @Override
    public Optional<ICloudServer> get(String name) {
        return Optional.ofNullable(servers.getOrDefault(name, servers.values().stream()
                .filter(s -> s.name().startsWith(name))
                .findFirst().orElse(null)
        ));
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
    public Collection<ICloudServer> getAll() {
        return widen(servers.values().stream().toList());
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