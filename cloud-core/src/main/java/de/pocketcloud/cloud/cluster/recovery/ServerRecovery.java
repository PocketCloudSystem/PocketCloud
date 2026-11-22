package de.pocketcloud.cloud.cluster.recovery;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.ClusterModule;
import de.pocketcloud.cloud.cluster.ServerAddresses;
import de.pocketcloud.cloud.cluster.recovery.packet.HeadKillServerPacket;
import de.pocketcloud.cloud.cluster.recovery.packet.HeadStateReportResultPacket;
import de.pocketcloud.cloud.cluster.recovery.packet.NodeStateReportPacket;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.util.CloudServerStorage;
import de.pocketcloud.cloud.server.util.ServerUtils;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.util.ProcessUtils;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.shared.component.data.CloudServerData;
import io.netty.channel.Channel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Recovery after node failures.
 * <ul>
 *     <li>Persists the servers this node runs in {@code storage/internal/cluster/local-servers.json}
 *     (name, uuid, template, id, port, pid, start time, status), also in single-node mode.</li>
 *     <li>On start, servers of the last run whose process still lives become candidates. Without a cluster they are
 *     adopted right away. In a cluster the node sends a {@link NodeStateReportPacket}; the head answers per server
 *     with ADOPT (it was not written off and the name is still its own) or KILL. Adopted servers get their port,
 *     id and process back and wait (STARTING) for their bridge to reconnect; otherwise the startup timeout kills them.</li>
 *     <li>Every node remembers the servers it wrote off because their node died. When such a server shows up again
 *     (a node returns, a partition heals) or a node reports a server whose name belongs to a server on another node,
 *     the receivers ignore it and the head tells the owning node to kill it ({@link HeadKillServerPacket}).</li>
 * </ul>
 */
public final class ServerRecovery implements ClusterModule {

    static final String ADOPT = "ADOPT";
    static final String KILL = "KILL";
    private static final long REPORT_INTERVAL_MS = 5_000;
    private static final long PROTECT_MS = 30_000;
    static final String STAT_WRITTEN_OFF = "writtenOff";

    private final Path file = PocketCloudPaths.storage().internal().with("cluster").asPath().resolve("local-servers.json");
    private final Map<String, LocalServerRecord> candidates = new ConcurrentHashMap<>(); // uuid → record
    private final Map<String, String> decisions = new ConcurrentHashMap<>(); // uuid → ADOPT/KILL
    private final WrittenOffList writtenOff = new WrittenOffList(PocketCloudPaths.storage().internal().with("cluster").asPath().resolve("written-off.json"));
    private final Map<String, Long> protectedServers = new ConcurrentHashMap<>(); // node/name → until
    private final Map<String, Long> killOrders = new ConcurrentHashMap<>(); // uuid → last order

    private ScheduledExecutorService executor;
    private volatile boolean ready;
    private volatile boolean dirty = true;
    private volatile String lastWritten = "";
    private long loadedAt;
    private long lastReportAt;

    public static Optional<ServerRecovery> get() {
        return PocketCloud.instance().cluster().modules().stream()
                .filter(ServerRecovery.class::isInstance).map(ServerRecovery.class::cast).findFirst();
    }

    @Override
    public Collection<Class<? extends Packet>> packets() {
        return List.of(NodeStateReportPacket.class, HeadStateReportResultPacket.class, HeadKillServerPacket.class);
    }

    // ------------------------------------------------------------------ lifecycle (runs with and without a cluster)

    /** Reads the servers of the last run. Called from {@link ClusterManager#init()}, before the cluster connects. */
    public void load() {
        loadedAt = System.currentTimeMillis();
        writtenOff.load();
        if (!Files.isRegularFile(file)) return;
        try {
            Map<String, Object> root = FileUtils.decodeJsonFile(file, new TypeToken<Map<String, Object>>() {});
            Object list = root == null ? null : root.get("servers");
            if (!(list instanceof List<?> entries)) return;
            List<LocalServerRecord> dead = new ArrayList<>();
            for (Object entry : entries) {
                if (!(entry instanceof Map<?, ?> map)) continue;
                @SuppressWarnings("unchecked")
                LocalServerRecord record = LocalServerRecord.read((Map<String, Object>) map);
                if (record.alive()) candidates.put(record.uuid, record);
                else dead.add(record);
            }
            if (!candidates.isEmpty()) {
                CloudLogger.get().info("Found §b{} §rserver(s) of the last run that are still running: §b{}", candidates.size(),
                        String.join("§8, §b", candidates.values().stream().map(LocalServerRecord::toString).toList()));
            }
            for (LocalServerRecord record : dead) cleanup(record, false);
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to read {}", e, file.toString());
        }
    }

    /** Templates are loaded: start adopting. Called once from {@code PocketCloud} before the ticker starts. */
    public void ready() {
        ready = true;
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PocketCloud-Recovery");
            t.setDaemon(true);
            return t;
        });

        // Single node: nobody else can have replaced the servers, adopt them before auto-scaling runs.
        if (!cluster().clustered()) {
            for (LocalServerRecord record : List.copyOf(candidates.values())) apply(record, ADOPT, "single node");
        }

        executor.scheduleWithFixedDelay(this::tickSafe, 1, 1, TimeUnit.SECONDS);
    }

    /** Clean shutdown: the cloud stops all its servers, so candidates are stopped too. */
    public void close() {
        if (executor != null) executor.shutdownNow();
        for (LocalServerRecord record : List.copyOf(candidates.values())) {
            candidates.remove(record.uuid);
            cleanup(record, true);
        }
        dirty = true;
        flush();
    }

    /** A local server changed (status, new, removed): rewrite the file soon. */
    public void markDirty() {
        dirty = true;
    }

    private void tickSafe() {
        try {
            tick();
        } catch (Throwable t) {
            CloudLogger.get().exception("Server recovery tick failed", t);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        writtenOff.expire(now);
        writtenOff.flush();
        protectedServers.values().removeIf(until -> until < now);
        killOrders.values().removeIf(time -> now - time > 60_000);

        for (LocalServerRecord record : List.copyOf(candidates.values())) {
            if (!record.alive()) {
                candidates.remove(record.uuid);
                decisions.remove(record.uuid);
                CloudLogger.get().info("Server §b{} §rof the last run has exited, cleaning up.", record.name);
                cleanup(record, false);
                dirty = true;
            }
        }

        for (Map.Entry<String, String> decision : List.copyOf(decisions.entrySet())) {
            decisions.remove(decision.getKey());
            LocalServerRecord record = candidates.get(decision.getKey());
            if (record != null) apply(record, decision.getValue(), "decided by the head");
        }

        if (!candidates.isEmpty()) {
            ClusterManager cluster = cluster();
            if (!cluster.clustered()) {
                for (LocalServerRecord record : List.copyOf(candidates.values())) apply(record, ADOPT, "single node");
            } else if (cluster.isHead()) {
                for (LocalServerRecord record : List.copyOf(candidates.values())) apply(record, decide(cluster.nodeName(), record), "this node is head");
            } else if (now - lastReportAt >= REPORT_INTERVAL_MS) {
                String head = cluster.election().head();
                if (head != null && !head.isEmpty() && cluster.send(head, report())) lastReportAt = now;
            }

            long giveUpAfter = (reconnectTimeoutSeconds() + 30) * 1000L;
            if (now - loadedAt > giveUpAfter) {
                for (LocalServerRecord record : List.copyOf(candidates.values())) {
                    CloudLogger.get().warn("No head decided about §b{} §rin time, stopping it.", record.name);
                    candidates.remove(record.uuid);
                    cleanup(record, true);
                }
                dirty = true;
            }
        }

        flush();
    }

    // ------------------------------------------------------------------ adoption (node side)

    private void apply(LocalServerRecord record, String decision, String by) {
        if (candidates.remove(record.uuid) == null) return;
        dirty = true;
        if (KILL.equals(decision)) {
            CloudLogger.get().warn("Server §b{} §rof the last run was §cwritten off §ror replaced §8(§r{}§8)§r, killing it §8(§rpid {}§8)§r.", record.name, by, record.pid);
            cleanup(record, true);
            return;
        }

        var servers = PocketCloud.instance().servers();
        Template template = (Template) PocketCloud.instance().templates().get(record.template).orElse(null);
        if (template == null || servers.get(record.name).isPresent()) {
            CloudLogger.get().warn("Cannot adopt §b{}§r: {}. Killing it.", record.name, template == null ? "its template does not exist" : "the name is taken");
            cleanup(record, true);
            return;
        }

        UUID uuid = UUID.fromString(record.uuid);
        ServerUtils.claimPort(record.port);
        CloudServer server = new CloudServer(record.id, uuid, template.name(),
                new CloudServerData(uuid, ServerAddresses.publicAddress(), record.port, template.settings().maxPlayerCount()),
                new CloudServerStorage(uuid));
        server.data().processId(record.pid);
        // STARTING until the bridge reconnects; the startup timeout kills the process if it never does.
        server.startTime(Instant.now());
        if (!servers.adopt(server)) {
            ServerUtils.removePort(record.port);
            cleanup(record, true);
            return;
        }
        server.status(ServerStatus.STARTING);
        CloudLogger.get().success("§aAdopted §b{} §rfrom the last run §8(§rpid {}, port {}, {}§8)§r. Waiting for its bridge to reconnect...",
                record.name, record.pid, record.port, by);
    }

    /** Kills the process (if asked) and removes the temporary server directory. */
    private void cleanup(LocalServerRecord record, boolean kill) {
        Runnable task = () -> {
            if (kill && record.alive()) {
                ProcessUtils.kill(record.pid, true);
                ProcessHandle.of(record.pid).ifPresent(handle -> {
                    try {
                        handle.onExit().get(5, TimeUnit.SECONDS);
                    } catch (Exception ignored) {
                    }
                });
            }
            Template template = (Template) PocketCloud.instance().templates().get(record.template).orElse(null);
            if (template != null && (template.settings().staticServers() || !template.settings().deleteOnStop())) return;
            Path dir = PocketCloudPaths.tmp().with(record.name + "_" + record.uuid).asPath();
            if (Files.isDirectory(dir) && !record.alive()) FileUtils.removeDirectory(dir);
        };
        if (executor != null && !executor.isShutdown()) executor.execute(task);
        else task.run();
    }

    private NodeStateReportPacket report() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (LocalServerRecord record : candidates.values()) list.add(record.write());
        return new NodeStateReportPacket(list);
    }

    // ------------------------------------------------------------------ head side

    /**
     * ADOPT if the head still knows the server on that node with the same uuid, or does not know the name and did
     * not write the server off. KILL if the server was written off or its name belongs to another server now.
     */
    private String decide(String from, LocalServerRecord record) {
        CloudServer existing = (CloudServer) PocketCloud.instance().servers().get(record.name).orElse(null);
        if (existing != null) {
            return nodeOf(existing).equals(from) && existing.uuid().toString().equals(record.uuid) ? ADOPT : KILL;
        }
        return writtenOff.contains(record.uuid) ? KILL : ADOPT;
    }

    private void handleReport(String from, NodeStateReportPacket packet) {
        ClusterManager cluster = cluster();
        long until = System.currentTimeMillis() + PROTECT_MS;
        Map<String, String> result = new HashMap<>();
        for (Map<String, Object> entry : packet.getServers()) {
            LocalServerRecord record = LocalServerRecord.read(entry);
            // Keep the server in the cache while the node decides, its first full report does not contain it yet.
            protectedServers.put(from + "/" + record.name, until);
            if (cluster.isHead()) {
                String decision = decide(from, record);
                result.put(record.uuid, decision);
                if (KILL.equals(decision)) protectedServers.remove(from + "/" + record.name);
                if (cluster.throttle("report:" + from + "/" + record.uuid)) CloudLogger.get().info("Node §b{} §rstill runs §b{} §rfrom before its restart: {}", from, record.name,
                        ADOPT.equals(decision) ? "§aadopt" : "§ckill §r(" + killReason(record) + ")");
            }
        }
        if (cluster.isHead() && !result.isEmpty()) cluster.send(from, new HeadStateReportResultPacket(cluster.election().term(), result));
    }

    // ------------------------------------------------------------------ hooks from ClusterManager

    /** Called right before this node sends its full server report to a peer that just connected. */
    public void beforeFullReport(String peer) {
        if (!candidates.isEmpty()) cluster().send(peer, report());
    }

    /** Whether the fullReport of {@code node} may drop the server: false while the node is still deciding about it. */
    public boolean isProtected(String node, String serverName) {
        Long until = protectedServers.get(node + "/" + serverName);
        return until != null && until >= System.currentTimeMillis();
    }

    /**
     * Whether a server state reported by {@code from} may be applied to the cache. The head orders the node to
     * kill servers that were written off or whose name belongs to a server on another node.
     */
    public boolean acceptRemote(String from, CloudServer incoming) {
        CloudServer existing = (CloudServer) PocketCloud.instance().servers().get(incoming.name()).orElse(null);
        String uuid = incoming.uuid().toString();
        if (existing != null) {
            String owner = nodeOf(existing);
            if (owner.equals(from)) {
                protectedServers.remove(from + "/" + incoming.name());
                return true; // the owning node is the authority for its own servers
            }
            if (incoming.status().isOffline() || incoming.status().isStopping()) return false;
            if (cluster().throttle("duplicate:" + from + "/" + incoming.name())) {
                CloudLogger.get().warn("Node §b{} §rreports §b{}§r, but that name belongs to a server on node §b{}§r. Ignoring it.",
                        from, incoming.name(), owner);
            }
            if (cluster().isHead()) orderKill(from, incoming, "name taken by the server on " + owner);
            return false;
        }

        protectedServers.remove(from + "/" + incoming.name());
        if (writtenOff.contains(uuid)) {
            // Only the head decides; it will order a kill. The entry stays, so a later head knows it too.
            if (!cluster().isHead()) return true;
            if (incoming.status().isOffline() || incoming.status().isStopping()) return false;
            orderKill(from, incoming, "written off after its node was gone");
            return false;
        }
        return true;
    }

    /** Only removals by the owning node count (another node may own a server with that name now). */
    public boolean acceptRemoval(String from, String serverName) {
        CloudServer existing = (CloudServer) PocketCloud.instance().servers().get(serverName).orElse(null);
        return existing != null && nodeOf(existing).equals(from);
    }

    private void orderKill(String node, CloudServer server, String reason) {
        String uuid = server.uuid().toString();
        Long last = killOrders.get(uuid);
        if (last != null && System.currentTimeMillis() - last < 5_000) return;
        killOrders.put(uuid, System.currentTimeMillis());
        CloudLogger.get().warn("§cStopping §b{} §ron node §b{}§r: {}.", server.name(), node, reason);
        cluster().send(node, new HeadKillServerPacket(server.name(), uuid, reason));
    }

    // ------------------------------------------------------------------ module callbacks

    @Override
    public boolean onPacket(String from, Channel channel, CloudPacket packet) {
        switch (packet) {
            case NodeStateReportPacket report -> handleReport(from, report);
            case HeadStateReportResultPacket result -> {
                if (result.getTerm() < cluster().election().term()) return true;
                decisions.putAll(result.getDecisions());
                if (ready && executor != null) executor.execute(this::tickSafe);
            }
            case HeadKillServerPacket kill -> handleKill(from, kill);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void handleKill(String from, HeadKillServerPacket packet) {
        LocalServerRecord candidate = candidates.remove(packet.getUuid());
        if (candidate != null) {
            CloudLogger.get().warn("Head §b{} §rorders to kill §b{}§r: {}.", from, candidate.name, packet.getReason());
            cleanup(candidate, true);
            dirty = true;
            return;
        }
        PocketCloud.instance().servers().get(packet.getServerName()).ifPresent(server -> {
            if (!(server instanceof CloudServer cs) || cs.remote() || !cs.uuid().toString().equals(packet.getUuid())) return;
            CloudLogger.get().warn("Head §b{} §rorders to stop the duplicate §b{}§r: {}.", from, cs.name(), packet.getReason());
            cs.stop(true);
        });
    }

    @Override
    public void onPeerGone(String name) {
        // Called before the servers of the dead node are dropped: remember them as written off.
        long now = System.currentTimeMillis();
        for (CloudServer server : PocketCloud.instance().servers().serversOfNode(name)) writtenOff.add(server.uuid().toString(), server.name(), name, now);
        writtenOff.flush();
    }

    @Override
    public void heartbeatStats(Map<String, Object> stats) {
        if (writtenOff.size() > 0) stats.put(STAT_WRITTEN_OFF, writtenOff.write());
    }

    @Override
    public void onHeartbeat(String from, Map<String, Object> stats) {
        if (stats == null) return;
        List<String> added = writtenOff.merge(stats.get(STAT_WRITTEN_OFF));
        if (!added.isEmpty()) {
            writtenOff.flush();
            CloudLogger.get().info("Learned §b{} §rwritten-off server(s) from node §b{}§r: §b{}", added.size(), from, String.join("§8, §b", added));
        }
    }

    private String killReason(LocalServerRecord record) {
        CloudServer existing = (CloudServer) PocketCloud.instance().servers().get(record.name).orElse(null);
        if (existing != null) return "name now belongs to the server on " + nodeOf(existing);
        WrittenOffList.Entry entry = writtenOff.get(record.uuid);
        return entry == null ? "written off" : "written off at " + java.time.LocalTime.ofInstant(Instant.ofEpochMilli(entry.time()), java.time.ZoneId.systemDefault()).withNano(0) + " after node " + entry.node() + " was gone";
    }

    // ------------------------------------------------------------------ persistence

    private void flush() {
        if (!dirty) return;
        dirty = false;
        try {
            List<Map<String, Object>> list = new ArrayList<>();
            for (CloudServer server : PocketCloud.instance().servers().localServers()) {
                if (server.data().processId() == null || server.status().isOffline()) continue;
                Map<String, Object> entry = LocalServerRecord.of(server).write();
                entry.put("startTime", server.startTime() == null ? 0 : server.startTime().toEpochMilli());
                list.add(entry);
            }
            for (LocalServerRecord record : candidates.values()) list.add(record.write());

            Map<String, Object> root = new LinkedHashMap<>();
            root.put("node", cluster().nodeName());
            root.put("servers", list);
            String json = FileUtils.PRETTY_GSON.toJson(root);
            if (json.equals(lastWritten)) return;
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, json);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            lastWritten = json;
        } catch (Exception e) {
            dirty = true;
            CloudLogger.get().exception("Failed to write {}", e, file.toString());
        }
    }

    // ------------------------------------------------------------------ helpers

    private String nodeOf(CloudServer server) {
        return server.nodeName() == null ? cluster().nodeName() : server.nodeName();
    }

    private static ClusterManager cluster() {
        return PocketCloud.instance().cluster();
    }

    /**
     * Startup: empties {@code tmp/} except the directories of servers of the last run that still run
     * (they may be adopted). Runs before any service exists.
     */
    public static void cleanTmpFolder() {
        Path tmp = PocketCloudPaths.tmp().asPath();
        Path file = PocketCloudPaths.storage().internal().with("cluster").asPath().resolve("local-servers.json");
        Set<String> keep = new HashSet<>();
        try {
            if (Files.isRegularFile(file)) {
                Map<String, Object> root = FileUtils.decodeJsonFile(file, new TypeToken<Map<String, Object>>() {});
                if (root != null && root.get("servers") instanceof List<?> entries) {
                    for (Object entry : entries) {
                        if (!(entry instanceof Map<?, ?> map)) continue;
                        @SuppressWarnings("unchecked")
                        LocalServerRecord record = LocalServerRecord.read((Map<String, Object>) map);
                        if (record.alive()) keep.add(record.name + "_" + record.uuid);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to read " + file + ": " + e.getMessage());
        }

        if (keep.isEmpty() || !Files.isDirectory(tmp)) {
            FileUtils.removeDirectory(tmp);
            return;
        }
        try (var children = Files.list(tmp)) {
            for (Path child : children.toList()) {
                if (keep.contains(child.getFileName().toString())) continue;
                if (Files.isDirectory(child)) FileUtils.removeDirectory(child);
                else Files.deleteIfExists(child);
            }
        } catch (Exception e) {
            System.err.println("Failed to clean " + tmp + ": " + e.getMessage());
        }
    }

    public static int reconnectTimeoutSeconds() {
        return Math.max(10, PocketCloud.instance().serverSettingsConfig().bridgeReconnectTimeout());
    }
}
