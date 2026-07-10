package de.pocketcloud.cloud.server;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.network.packet.impl.ServerSyncPacket;
import de.pocketcloud.cloud.server.util.CloudServerData;
import de.pocketcloud.cloud.server.start.ServerStartMethod;
import de.pocketcloud.cloud.server.util.ServerStartMethods;
import de.pocketcloud.cloud.server.util.ServerUtils;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.TemplateType;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.common.lifecycle.Tickable;
import de.pocketcloud.cloud.util.benchmark.Benchmark;
import de.pocketcloud.cloud.util.concurrent.Promise;
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
public final class CloudServerManager implements Tickable {

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

    @Getter
    @Accessors(fluent = true)
    private static CloudServerManager instance = null;

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

    public CloudServerManager() {
        instance = this;
    }

    public List<String> start(Template template, int count) {
        List<String> startedServers = new ArrayList<>();

        if (!checkCapacity(template)) {
            CloudLogger.get().warn("Failed to start any more servers of §b{} §rdue to the max amount of servers already being reached.", template.name());
            return startedServers;
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
                new CloudServerData(uuid, port, template.settings().getMaxPlayerCount())
            );

            latestServerStartTimes.put(template.name(), server.name());
            add(server);
            serverPrepareQueue.offer(server);
            startedServers.add(server.name());
        }

        return startedServers;
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

    public List<CloudServer> stop(CloudServer source, boolean force) {
        List<CloudServer> affectedServers = new ArrayList<>(Collections.singleton(source));
        affectedServers.forEach(server -> server.stop(force));
        return affectedServers;
    }

    public List<CloudServer> stop(Template template, boolean force) {
        List<CloudServer> affectedServers = getAll(template);
        affectedServers.forEach(server -> server.stop(force));
        return affectedServers;
    }

    public List<CloudServer> stop(ServerGroup group, boolean force) {
        List<CloudServer> affectedServers = getAll(group);
        affectedServers.forEach(server -> server.stop(force));
        return affectedServers;
    }

    public List<CloudServer> stop(TemplateType templateType, boolean force) {
        List<CloudServer> affectedServers = getAll(templateType);
        affectedServers.forEach(server -> server.stop(force));
        return affectedServers;
    }

    public List<CloudServer> stop(String name, boolean force) {
        List<CloudServer> affectedServers = new ArrayList<>();
        if (servers.containsKey(name)) affectedServers.add(servers.get(name));
        affectedServers.forEach(server -> server.stop(force));
        return affectedServers;
    }

    public List<CloudServer> stopAll() {
        return stopAll(false);
    }

    public List<CloudServer> stopAll(boolean force) {
        List<CloudServer> all = getAll();
        all.forEach(server -> server.stop(force));
        return all;
    }

    public void add(CloudServer server) {
        servers.putIfAbsent(server.name(), server);
        ServerUtils.addId(server.template(), server.id());
        ServerUtils.addPort(server.serverData().port());
    }

    public void remove(CloudServer server) {
        servers.remove(server.name());
        ServerUtils.removeId(server.template(), server.id());
        ServerUtils.removePort(server.serverData().port());
        this.lastServerStopTime = System.currentTimeMillis();
        ServerSyncPacket.create(server, true).broadcastPacket();
    }

    public boolean checkCapacity(Template template) {
        return getAll(template).size() < template.settings().getMaxServerCount();
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
                List<CloudServer> batch = new ArrayList<>();
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
                .filter(s -> s.template().settings().isLobby() && s.playerCount() < s.serverData().maxPlayers())
                .min(Comparator.comparingInt(CloudServer::playerCount));
    }

    public Optional<CloudServer> getLatest(Template template) {
        String latestName = latestServerStartTimes.get(template.name());
        if (latestName == null) return Optional.empty();
        return Optional.ofNullable(servers.getOrDefault(latestName, null));
    }

    public List<CloudServer> getAll() {
        return new ArrayList<>(servers.values());
    }

    public List<CloudServer> getAll(Template template) {
        return servers.values().stream().filter(s -> s.templateName().equals(template.name())).toList();
    }

    public List<CloudServer> getAll(TemplateType type) {
        return servers.values().stream().filter(s -> s.template().templateType().name().equals(type.name())).toList();
    }

    public List<CloudServer> getAll(ServerGroup group) {
        return servers.values().stream().filter(s -> s.template().parentGroups().stream().anyMatch(p -> p.name().equals(group.name()))).toList();
    }
}