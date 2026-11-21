package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.net.ClusterNetwork;
import de.pocketcloud.cloud.cluster.net.packet.*;
import de.pocketcloud.cloud.cluster.redis.RedisConnection;
import de.pocketcloud.cloud.cluster.security.ClusterCertificates;
import de.pocketcloud.cloud.cluster.security.JoinToken;
import de.pocketcloud.cloud.cluster.state.ClusterStateStore;
import de.pocketcloud.cloud.cluster.state.LocalStateStore;
import de.pocketcloud.cloud.cluster.state.RedisStateStore;
import de.pocketcloud.cloud.cluster.state.StateKeys;
import de.pocketcloud.cloud.cluster.storage.LocalTemplateStorage;
import de.pocketcloud.cloud.cluster.storage.TemplateStorage;
import de.pocketcloud.cloud.config.sub.RedisConfiguration;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.VersionInfo;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.RequestPacket;
import de.pocketcloud.network.packet.ResponsePacket;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Entry point of the cluster layer.
 * <p>
 * Without cluster certificates the node runs alone and is always head (classic single-server mode).
 * With certificates ({@code node add} on an existing node, {@code --join <token>} on a new one) it opens
 * the TLS cluster port, connects to every member, exchanges heartbeats and takes part in the head election.
 * <p>
 * Also owns the shared {@link ClusterStateStore}, the optional {@link RedisConnection} and the {@link TemplateStorage}.
 */
@Accessors(fluent = true)
public final class ClusterManager {

    public static final int PROTOCOL_VERSION = 1;

    @Getter
    private final TemplateStorage templateStorage = new LocalTemplateStorage();
    @Getter
    private volatile ClusterStateStore state = new LocalStateStore();
    private volatile RedisConnection redis;

    private static final long SCHEDULE_SETTLE_MS = 3_000;

    private final Path directory = PocketCloudPaths.storage().internal().with("cluster").asPath();
    @Getter
    private final ClusterCertificates certificates = new ClusterCertificates(directory);
    @Getter
    private final ClusterMembers members = new ClusterMembers(directory.resolve("members.json"));
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private final Set<String> dialing = ConcurrentHashMap.newKeySet();

    /** Features on top of the node protocol. Add new modules here. */
    private final List<ClusterModule> modules = List.of(new de.pocketcloud.cloud.cluster.sync.TemplateReplicator());

    private LocalNodeHandle localNode;
    @Getter
    private HeadElection election;
    private ClusterNetwork network;
    private ScheduledExecutorService scheduler;
    @Getter
    private boolean clustered;

    public void init() {
        NodeConfig config = nodeConfig();
        localNode = new LocalNodeHandle(config.name());
        election = new HeadElection(this, directory.resolve("term.json"));

        connectRedis();
        processJoinToken();

        try {
            members.load();
            clustered = certificates.load();
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to load the cluster certificates, running as single node", e);
            clustered = false;
        }

        if (!clustered) {
            election.forceSingleHead();
            return;
        }

        startCluster();
    }

    private void startCluster() {
        NodeConfig config = nodeConfig();
        String certName = ClusterCertificates.commonName(certificates.nodeCertificate());
        if (!config.name().equals(certName)) {
            CloudLogger.get().error("node.yml name §b{} §rdoes not match the node certificate §b{}§r. Cluster disabled.", config.name(), certName);
            clustered = false;
            election.forceSingleHead();
            return;
        }

        try {
            // A freshly joined node waits for the member list of the cluster (it contains this node).
            // Only a node without seeds creates the list itself.
            if (!members.contains(config.name()) && config.seeds().isEmpty()) {
                members.put(new ClusterMembers.Member(config.name(), config.advertisedAddress(), config.clusterPort()));
            }
            network = new ClusterNetwork(this, certificates);
            for (ClusterModule module : modules) module.packets().forEach(network.registry()::register);
            network.start(config.clusterBindAddress(), config.clusterPort());
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to start the cluster port {}:{}", e, config.clusterBindAddress(), config.clusterPort());
            clustered = false;
            election.forceSingleHead();
            return;
        }

        CloudLogger.get().info("Cluster port §b{}:{} §ropened §8(§rnode §b{}§r, §b{} §rmember(s)§8)§r.", config.clusterBindAddress(), config.clusterPort(), config.name(), members.size());
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PocketCloud-Cluster");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(safe(this::dial), 0, 2, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(safe(this::heartbeat), 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(safe(() -> election.tick()), 250, 250, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(safe(() -> network.requests().tick(0)), 1, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(safe(this::dropServersOfDeadNodes), 2, 1, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(safe(() -> broadcast(() -> fullReport())), 10, 10, TimeUnit.SECONDS);

        for (ClusterModule module : modules) {
            try {
                module.start(this);
            } catch (Exception e) {
                CloudLogger.get().exception("Failed to start cluster module {}", e, module.getClass().getSimpleName());
            }
        }
    }

    private Runnable safe(Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (Throwable t) {
                CloudLogger.get().exception("Cluster task failed", t);
            }
        };
    }

    public void close() {
        if (network != null) {
            for (ClusterModule module : modules) {
                try {
                    module.stop();
                } catch (Exception e) {
                    CloudLogger.get().exception("Failed to stop cluster module {}", e, module.getClass().getSimpleName());
                }
            }
        }
        if (scheduler != null) scheduler.shutdownNow();
        if (network != null) {
            for (Peer peer : peers.values()) {
                if (peer.connected()) peer.channel().writeAndFlush(new NodeLeavePacket()).awaitUninterruptibly(500);
            }
            network.close();
            network = null;
        }

        RedisConnection connection = redis;
        if (connection != null) {
            redis = null;
            try {
                connection.close();
            } catch (RuntimeException e) {
                CloudLogger.get().exception("Failed to close the Redis connection", e);
            }
        }
    }

    // ------------------------------------------------------------------ node add / join

    /**
     * Adds a node to the cluster and returns its join token. Creates the cluster CA first if this
     * node is not part of a cluster yet.
     */
    public synchronized String addNode(String name, String address, int port) throws Exception {
        NodeConfig config = nodeConfig();
        if (name.equals(config.name())) throw new IllegalArgumentException("That is the name of this node.");
        if (!name.matches("[a-zA-Z0-9_\\-]{1,32}")) throw new IllegalArgumentException("Invalid node name.");

        if (!clustered) {
            CloudLogger.get().info("Creating the cluster CA...");
            certificates.initialize(config.name(), PocketCloud.instance().config().cloudName());
            members.put(new ClusterMembers.Member(config.name(), config.advertisedAddress(), config.clusterPort()));
            clustered = true;
            startCluster();
        }

        if (!certificates.hasCaKey()) throw new IllegalStateException("Only the node holding the cluster CA key (ca.key) can add nodes.");

        ClusterCertificates.IssuedNode issued = certificates.issue(name);
        members.put(new ClusterMembers.Member(name, address, port));

        List<String> seeds = new ArrayList<>();
        for (ClusterMembers.Member m : members.all()) if (!m.name().equals(name)) seeds.add(m.endpoint());

        return new JoinToken(name,
                ClusterCertificates.toPem(certificates.caCertificate()),
                ClusterCertificates.toPem(issued.certificate()),
                ClusterCertificates.toPem(issued.key()),
                seeds).encode() + "@" + address + ":" + port;
    }

    public synchronized boolean removeNode(String name) throws Exception {
        if (name.equals(nodeName())) throw new IllegalArgumentException("A node cannot remove itself.");
        boolean removed = members.remove(name);
        Peer peer = peers.remove(name);
        if (peer != null && peer.connected()) peer.channel().close();
        return removed;
    }

    /**
     * {@code --join <token>} stores the token in {@code join-token}; it is applied once on the next start.
     */
    private void processJoinToken() {
        Path file = directory.resolve("join-token");
        if (!Files.isRegularFile(file)) return;
        try {
            String raw = Files.readString(file).trim();
            String advertised = null;
            int at = raw.lastIndexOf('@');
            if (at > 0) {
                advertised = raw.substring(at + 1);
                raw = raw.substring(0, at);
            }

            JoinToken token = JoinToken.decode(raw);
            certificates.install(token);

            NodeConfig config = nodeConfig();
            config.name(token.nodeName());
            config.seeds(new ArrayList<>(token.seeds()));
            if (advertised != null && advertised.contains(":")) {
                config.advertisedAddress(advertised.substring(0, advertised.lastIndexOf(':')));
                config.clusterPort(Integer.parseInt(advertised.substring(advertised.lastIndexOf(':') + 1)));
            }
            config.save();
            localNode = new LocalNodeHandle(config.name());

            Files.delete(file);
            CloudLogger.get().success("Joined the cluster as §b{}§r. Seeds: §b{}", token.nodeName(), String.join("§8, §b", token.seeds()));
        } catch (Exception e) {
            CloudLogger.get().exception("Invalid join token", e);
        }
    }

    public static void storeJoinToken(String token) throws Exception {
        Path dir = PocketCloudPaths.storage().internal().with("cluster").asPath();
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("join-token"), token);
    }

    // ------------------------------------------------------------------ connections

    private void dial() {
        Set<String> targets = new LinkedHashSet<>();
        for (ClusterMembers.Member m : members.all()) {
            if (m.name().equals(nodeName())) continue;
            Peer peer = peers.get(m.name());
            if (peer != null && peer.connected()) continue;
            targets.add(m.endpoint());
        }

        boolean anyConnected = peers.values().stream().anyMatch(Peer::connected);
        if (!anyConnected) targets.addAll(nodeConfig().seeds());

        String own = nodeConfig().advertisedAddress() + ":" + nodeConfig().clusterPort();
        targets.remove(own);

        for (String endpoint : targets) {
            if (!dialing.add(endpoint)) continue;
            int idx = endpoint.lastIndexOf(':');
            String host = endpoint.substring(0, idx);
            int port = Integer.parseInt(endpoint.substring(idx + 1));
            network.connect(host, port, endpoint).addListener(f -> {
                if (!f.isSuccess()) dialing.remove(endpoint);
            });
        }
    }

    /** Outbound connection is established (TLS done): say hello. */
    public void onConnected(Channel channel) {
        NodeHelloPacket hello = new NodeHelloPacket(identity(), PROTOCOL_VERSION);
        hello.then(response -> {
            if (response instanceof NodeHelloResponsePacket r) onHelloResponse(channel, r);
        }).failure((_, ex, reason) -> {
            channel.close();
        });
        channel.writeAndFlush(hello);
    }

    private void onHelloResponse(Channel channel, NodeHelloResponsePacket response) {
        String endpoint = channel.attr(ClusterNetwork.ENDPOINT).get();
        if (!response.isAccepted()) {
            if (throttle("rejected-by:" + endpoint)) {
                CloudLogger.get().warn("Node at §b{} §rrejected the connection: {}", endpoint, response.getReason());
            }
            channel.close();
            return;
        }

        NodeIdentity id = response.getIdentity();
        if (!id.name().equals(channel.attr(ClusterNetwork.CERT_NAME).get())) {
            CloudLogger.get().warn("Node at §b{} §rclaims to be §b{}§r, but its certificate says otherwise. Closing.", endpoint, id.name());
            channel.close();
            return;
        }

        adoptMembers(response.getMembers());
        register(channel, id, true);
    }

    public void onPacket(Channel channel, CloudPacket packet) throws Exception {
        if (packet instanceof NodeHelloPacket hello) {
            handleHello(channel, hello);
            return;
        }

        String from = channel.attr(ClusterNetwork.PEER_NAME).get();
        if (from == null) return;
        Peer peer = peers.get(from);
        if (peer == null || peer.channel() != channel) return;
        peer.lastSeen(System.currentTimeMillis());

        switch (packet) {
            case NodeHeartbeatPacket hb -> {
                peer.term(hb.getTerm());
                peer.head(hb.getHead());
                peer.state(hb.getState());
                peer.stats(hb.getStats());
                adoptMembers(hb.getMembers());
                election.observe(hb.getTerm(), hb.getHead());
            }
            case HeadClaimPacket claim -> respond(channel, claim, election.onClaim(from, claim));
            case HeadAnnouncePacket announce -> election.onAnnounce(from, announce.getTerm());
            case NodeLeavePacket _ -> {
                peer.lastSeen(0);
                election.peerLeft(from);
                CloudLogger.get().info("Node §b{} §rleft the cluster.", from);
                dropServersOfDeadNodes();
            }
            case HeadStartServerPacket start -> respond(channel, start, handleStartRequest(from, start));
            case NodeStopServerPacket stop -> PocketCloud.instance().servers().get(stop.getServerName()).ifPresent(server -> {
                if (server instanceof CloudServer cs && !cs.remote()) cs.stop(stop.isForce());
            });
            case NodeServerStatePacket report -> handleServerState(from, report);
            case NodeForwardStartPacket forward -> handleForwardedStart(channel, forward);
            default -> {
                for (ClusterModule module : modules) {
                    if (module.onPacket(from, channel, packet)) break;
                }
            }
        }
    }

    private void handleHello(Channel channel, NodeHelloPacket hello) {
        NodeIdentity id = hello.getIdentity();
        String certName = channel.attr(ClusterNetwork.CERT_NAME).get();
        String reason = null;
        if (hello.getProtocolVersion() != PROTOCOL_VERSION) reason = "protocol version " + hello.getProtocolVersion() + " != " + PROTOCOL_VERSION;
        else if (!id.name().equals(certName)) reason = "name does not match certificate";
        else if (id.name().equals(nodeName())) reason = "same name as this node";
        else if (!members.contains(id.name())) reason = "not a member of this cluster";

        if (reason != null) {
            if (throttle("reject:" + id.name())) {
                CloudLogger.get().warn("Rejected node connection from §b{} §8(§b{}§8)§r: {}", ClusterNetwork.remote(channel), id.name(), reason);
            }
            respond(channel, hello, new NodeHelloResponsePacket(false, reason, null, null));
            channel.close();
            return;
        }

        respond(channel, hello, new NodeHelloResponsePacket(true, null, identity(), members.write()));
        register(channel, id, false);
    }

    private void register(Channel channel, NodeIdentity id, boolean outbound) {
        if (channel.attr(ClusterNetwork.ENDPOINT).get() != null) dialing.remove(channel.attr(ClusterNetwork.ENDPOINT).get());
        Peer peer = peers.computeIfAbsent(id.name(), Peer::new);
        synchronized (peer) {
            Channel existing = peer.channel();
            if (existing != null && existing != channel && existing.isActive()) {
                // Both nodes opened a connection. Keep the one opened by the node with the smaller name.
                boolean keepNew = (nodeName().compareTo(id.name()) < 0) == outbound;
                if (!keepNew) {
                    channel.attr(ClusterNetwork.PEER_NAME).set(null);
                    channel.close();
                    return;
                }
                existing.attr(ClusterNetwork.PEER_NAME).set(null);
                existing.close();
            }

            channel.attr(ClusterNetwork.PEER_NAME).set(id.name());
            boolean wasAlive = peer.alive(System.currentTimeMillis());
            peer.channel(channel);
            peer.outbound(outbound);
            peer.identity(id);
            peer.term(id.term());
            peer.head(id.head());
            peer.lastSeen(System.currentTimeMillis());
            peer.serversDropped(false);
            if (!wasAlive) CloudLogger.get().info("Node §b{} §8(§b{}:{}§8) §ris §aconnected§r.", id.name(), id.address(), id.port());
        }

        network.send(channel, fullReport());
        for (ClusterModule module : modules) module.onPeerConnected(id.name());

        election.observe(id.term(), id.head());
    }

    public void onDisconnected(Channel channel) {
        String endpoint = channel.attr(ClusterNetwork.ENDPOINT).get();
        if (endpoint != null) dialing.remove(endpoint);
        String name = channel.attr(ClusterNetwork.PEER_NAME).get();
        if (name == null) return;
        Peer peer = peers.get(name);
        if (peer != null && peer.channel() == channel) {
            peer.channel(null);
            CloudLogger.get().debug("Connection to node {} closed", name);
        }
    }

    private void heartbeat() {
        Map<String, Object> stats = new HashMap<>();
        Runtime rt = Runtime.getRuntime();
        stats.put("usedMemoryMb", (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024);
        stats.put("maxMemoryMb", (long) nodeConfig().maxMemoryMb());
        stats.put("servers", PocketCloud.instance().servers().serverCount());
        String head = election.head();
        long term = election.term();
        String nodeState = election.state().name();
        Map<String, Object> memberData = members.write();
        for (Peer peer : peers.values()) {
            if (peer.connected()) network.send(peer.channel(), new NodeHeartbeatPacket(term, head, nodeState, stats, memberData));
        }
    }

    private void adoptMembers(Map<String, Object> data) {
        try {
            if (members.adoptIfNewer(data)) {
                CloudLogger.get().info("Member list updated §8(§b{} §rnode(s)§8)§r.", members.size());
                for (String name : List.copyOf(peers.keySet())) {
                    if (!members.contains(name)) {
                        Peer removed = peers.remove(name);
                        if (removed != null && removed.connected()) removed.channel().close();
                    }
                }
            }
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to store the member list", e);
        }
    }

    public List<ClusterModule> modules() {
        return modules;
    }

    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    /** Sends a packet to a connected node. Returns false if the node is not connected. */
    public boolean send(String peerName, CloudPacket packet) {
        Peer p = peers.get(peerName);
        if (p == null || !p.connected()) return false;
        network.send(p.channel(), packet);
        return true;
    }

    public void respond(Channel channel, RequestPacket request, ResponsePacket response) {
        response.setRequestId(request.getRequestId());
        network.send(channel, response);
    }

    public void sendRequest(String peerName, RequestPacket request, Consumer<ResponsePacket> onResponse) {
        Peer peer = peers.get(peerName);
        if (peer == null || !peer.connected()) return;
        request.then(onResponse).failure((_, _, _) -> {});
        network.send(peer.channel(), request);
    }

    public void broadcast(Supplier<CloudPacket> packet) {
        for (Peer peer : peers.values()) {
            if (peer.connected()) network.send(peer.channel(), packet.get());
        }
    }

    // ------------------------------------------------------------------ election support

    public List<String> aliveNodes(long now) {
        List<String> alive = new ArrayList<>();
        alive.add(nodeName());
        for (Peer peer : peers.values()) {
            if (members.contains(peer.name()) && peer.alive(now)) alive.add(peer.name());
        }
        return alive;
    }

    public int quorum() {
        int size = Math.max(1, members.size());
        return size / 2 + 1;
    }

    /** The node with the smallest (startedAt, name). */
    public String oldest(Collection<String> nodes) {
        return nodes.stream()
                .min(Comparator.<String>comparingLong(n -> n.equals(nodeName()) ? election.startedAt() : peerStartedAt(n))
                        .thenComparing(Comparator.naturalOrder()))
                .orElse(nodeName());
    }

    private long peerStartedAt(String name) {
        Peer peer = peers.get(name);
        return peer == null ? Long.MAX_VALUE : peer.startedAt();
    }

    /**
     * Writes term and head to Redis, but only if no newer term is stored there.
     *
     * @return false if Redis knows a newer term (this node must not become head)
     */
    boolean fenceHead(long term) {
        RedisConnection connection = redis;
        if (connection == null) return true;
        try {
            Long ok = connection.sync().eval(
                    "local cur = tonumber(redis.call('GET', KEYS[1]) or '0') " +
                            "if tonumber(ARGV[1]) > cur then " +
                            "redis.call('SET', KEYS[1], ARGV[1]) " +
                            "redis.call('HSET', KEYS[2], 'name', ARGV[2], 'term', ARGV[1], 'since', ARGV[3]) " +
                            "return 1 end return 0",
                    io.lettuce.core.ScriptOutputType.INTEGER,
                    new String[]{connection.key("term"), connection.key("head")},
                    String.valueOf(term), nodeName(), String.valueOf(System.currentTimeMillis()));
            if (ok != null && ok == 1L) return true;
            CloudLogger.get().warn("Redis already knows a newer head term than §b{}§r.", term);
            return false;
        } catch (RuntimeException e) {
            CloudLogger.get().error("Cannot become head while Redis is unreachable: {}", e.getMessage());
            return false;
        }
    }

    void onHeadChanged(String head) {
        // Hook for later phases (scheduling, state takeover).
    }

    private NodeIdentity identity() {
        NodeConfig config = nodeConfig();
        return new NodeIdentity(config.name(), config.advertisedAddress(), config.clusterPort(), election.startedAt(),
                election.term(), election.head(), VersionInfo.VERSION);
    }

    private final Map<String, Long> lastWarnings = new ConcurrentHashMap<>();

    /** Repeated connection warnings are logged at most once per minute. */
    public boolean throttle(String key) {
        long now = System.currentTimeMillis();
        Long last = lastWarnings.get(key);
        if (last != null && now - last < 60_000) return false;
        lastWarnings.put(key, now);
        return true;
    }

    // ------------------------------------------------------------------ accessors

    public String nodeName() {
        return nodeConfig().name();
    }

    public boolean isHead() {
        return election == null || election.isHead();
    }

    /**
     * Whether this node may schedule servers now: it is head and, in a cluster, has been head for a few
     * seconds so the followers' states (heartbeats) are known and servers are spread across the nodes.
     */
    public boolean canSchedule() {
        if (!isHead()) return false;
        if (election == null || !clustered) return true;
        return System.currentTimeMillis() - election.headSince() >= SCHEDULE_SETTLE_MS;
    }

    public Collection<Peer> peers() {
        return List.copyOf(peers.values());
    }

    public Optional<Peer> peer(String name) {
        return Optional.ofNullable(peers.get(name));
    }

    private NodeConfig nodeConfig() {
        return PocketCloud.instance().nodeConfig();
    }

    // ------------------------------------------------------------------ redis

    private void connectRedis() {
        RedisConfiguration config = PocketCloud.instance().config().redis();
        if (!config.enabled()) return;

        CloudLogger.get().info("Connecting to §bRedis §8(§b{}:{}§8)§r...", config.address(), config.port());
        try {
            redis = RedisConnection.open(config, PocketCloud.instance().config().cloudName());
            state = new RedisStateStore(redis);
            CloudLogger.get().success("§aConnected §rto §bRedis§r. §8(§rnamespace §b{}§8)", redis.keyPrefix());
            cleanupStaleState();
        } catch (RuntimeException e) {
            redis = null;
            state = new LocalStateStore();
            CloudLogger.get().error("§cCould not connect §rto §bRedis§r: {}", e.getMessage());
            CloudLogger.get().warn("Falling back to the §blocal state§r. This only works with a single node.");
        }
    }

    public Optional<RedisConnection> redisConnection() {
        return Optional.ofNullable(redis);
    }

    /**
     * Removes what this node left in the shared state after a crash or hard stop:
     * its servers, their reserved ids and the players that were on them.
     */
    private void cleanupStaleState() {
        String nodeServersKey = StateKeys.nodeServers(nodeName());
        Set<String> staleServers = state.members(nodeServersKey);
        if (staleServers.isEmpty()) return;

        for (String serverName : staleServers) {
            state.get(StateKeys.SERVERS, serverName).ifPresent(doc -> {
                Object template = doc.get("templateName");
                Object id = doc.get("id");
                if (template != null && id instanceof Number number) {
                    state.removeFromSet(StateKeys.ids(template.toString()), String.valueOf(number.intValue()));
                }
            });
            state.remove(StateKeys.SERVERS, serverName);
            state.removeFromSet(nodeServersKey, serverName);
        }

        int players = 0;
        for (Map.Entry<String, Map<String, Object>> entry : state.getAll(StateKeys.PLAYERS).entrySet()) {
            Object server = entry.getValue().get("currentServerName");
            Object proxy = entry.getValue().get("currentProxyName");
            if ((server != null && staleServers.contains(server.toString())) || (proxy != null && staleServers.contains(proxy.toString()))) {
                state.remove(StateKeys.PLAYERS, entry.getKey());
                players++;
            }
        }

        CloudLogger.get().info("Removed §b{} §rstale server(s) and §b{} §rplayer(s) of this node from the shared state.", staleServers.size(), players);
    }

    // ------------------------------------------------------------------ servers across nodes

    private NodeStartServerResultPacket handleStartRequest(String from, HeadStartServerPacket request) {
        if (request.getTerm() < election.term()) {
            return new NodeStartServerResultPacket(false, "stale head term " + request.getTerm() + " < " + election.term(), 0);
        }

        var template = PocketCloud.instance().templates().get(request.getTemplateName()).orElse(null);
        if (template == null) return new NodeStartServerResultPacket(false, "template " + request.getTemplateName() + " does not exist on " + nodeName(), 0);

        try {
            int port = PocketCloud.instance().servers().startLocal(template, request.getId(), UUID.fromString(request.getUuid()));
            CloudLogger.get().info("Head §b{} §rstarts §b{}-{} §ron this node.", from, template.name(), request.getId());
            return new NodeStartServerResultPacket(true, null, port);
        } catch (Exception e) {
            return new NodeStartServerResultPacket(false, e.getMessage(), 0);
        }
    }

    private void handleServerState(String from, NodeServerStatePacket packet) {
        var servers = PocketCloud.instance().servers();
        Set<String> reported = new HashSet<>();
        for (Map<String, Object> document : packet.getServers()) {
            try {
                CloudServer server = CloudServer.fromCluster(document);
                if (server.nodeName() == null || server.nodeName().equals(nodeName())) continue;
                reported.add(server.name());
                servers.applyRemoteServer(server);
            } catch (Exception e) {
                CloudLogger.get().exception("Invalid server state from node {}", e, from);
            }
        }

        for (String removed : packet.getRemoved()) servers.removeRemoteServer(removed);

        if (packet.isFullReport()) {
            for (CloudServer known : servers.serversOfNode(from)) {
                if (!reported.contains(known.name()) && !known.status().isStarting() && known.status() != de.pocketcloud.api.server.ServerStatus.PENDING) {
                    servers.removeRemoteServer(known.name());
                }
            }
        }
    }

    private void handleForwardedStart(Channel channel, NodeForwardStartPacket request) {
        var template = PocketCloud.instance().templates().get(request.getTemplateName()).orElse(null);
        if (!isHead() || template == null) {
            respond(channel, request, new HeadForwardStartResultPacket(List.of()));
            return;
        }

        PocketCloud.instance().servers().start(template, request.getCount())
                .thenSuccess(names -> respond(channel, request, new HeadForwardStartResultPacket(List.copyOf(names))))
                .failure(_ -> respond(channel, request, new HeadForwardStartResultPacket(List.of())));
    }

    /** Called by the owning node whenever one of its servers changes. */
    public void broadcastServerState(CloudServer server, boolean removed) {
        if (!clustered || network == null) return;
        Map<String, Object> document = removed ? null : server.writeForCluster();
        broadcast(() -> removed
                ? new NodeServerStatePacket(false, List.of(), List.of(server.name()))
                : new NodeServerStatePacket(false, List.of(document), List.of()));
    }

    private NodeServerStatePacket fullReport() {
        List<Map<String, Object>> documents = new ArrayList<>();
        for (CloudServer server : PocketCloud.instance().servers().localServers()) documents.add(server.writeForCluster());
        return new NodeServerStatePacket(true, documents, List.of());
    }

    private void dropServersOfDeadNodes() {
        long now = System.currentTimeMillis();
        for (Peer peer : peers.values()) {
            if (peer.alive(now) || peer.serversDropped()) continue;
            peer.serversDropped(true);
            for (ClusterModule module : modules) module.onPeerGone(peer.name());
            Collection<CloudServer> lost = PocketCloud.instance().servers().serversOfNode(peer.name());
            if (lost.isEmpty()) continue;
            CloudLogger.get().warn("Node §b{} §ris gone, its §b{} §rserver(s) are treated as offline: §b{}", peer.name(), lost.size(),
                    String.join("§8, §b", lost.stream().map(CloudServer::name).toList()));
            for (CloudServer server : lost) PocketCloud.instance().servers().removeRemoteServer(server.name());
        }
    }

    /** Head → node: start a server there. Resolves with the port the node reserved. */
    public Promise<Integer> requestRemoteStart(String node, String template, int id, UUID uuid) {
        Promise<Integer> promise = new Promise<>();
        Peer peer = peers.get(node);
        if (peer == null || !peer.connected()) {
            promise.reject(new IllegalStateException("node " + node + " is not connected"));
            return promise;
        }

        HeadStartServerPacket request = new HeadStartServerPacket(election.term(), template, id, uuid.toString());
        request.then(response -> {
            if (response instanceof NodeStartServerResultPacket r && r.isSuccess()) promise.resolve(r.getPort());
            else if (response instanceof NodeStartServerResultPacket r) promise.reject(new IllegalStateException(r.getError()));
            else promise.reject(new IllegalStateException("unexpected response"));
        }).failure((_, ex, reason) -> promise.reject(ex != null ? ex : new IllegalStateException(String.valueOf(reason))));
        network.send(peer.channel(), request);
        return promise;
    }

    /** Non-head → head: start servers. */
    public Promise<Collection<String>> forwardStart(String template, int count) {
        String head = election.head();
        Peer peer = head == null ? null : peers.get(head);
        if (peer == null || !peer.connected()) {
            CloudLogger.get().warn("Cannot start §b{}§r: there is §cno head §rright now.", template);
            return Promise.resolved(List.of());
        }

        Promise<Collection<String>> promise = new Promise<>();
        NodeForwardStartPacket request = new NodeForwardStartPacket(template, count);
        request.then(response -> promise.resolve(response instanceof HeadForwardStartResultPacket r ? r.getServers() : List.of()))
                .failure((_, ex, reason) -> promise.reject(ex != null ? ex : new IllegalStateException(String.valueOf(reason))));
        network.send(peer.channel(), request);
        CloudLogger.get().info("Forwarded the start of §b{}x {} §rto head §b{}§r.", count, template, head);
        return promise;
    }

    /** Any node → owning node: stop a server there. */
    public void forwardStop(String node, String server, boolean force) {
        Peer peer = peers.get(node);
        if (peer == null || !peer.connected()) {
            CloudLogger.get().warn("Cannot stop §b{}§r: node §b{} §ris not connected.", server, node);
            return;
        }
        network.send(peer.channel(), new NodeStopServerPacket(server, force));
        CloudLogger.get().info("§cStopping §b{} §ron node §b{}§r...", server, node);
    }

    // ------------------------------------------------------------------ node handles

    private final Map<String, RemoteNodeHandle> remoteHandles = new ConcurrentHashMap<>();

    public LocalNodeHandle localNode() {
        return localNode;
    }

    public Optional<NodeHandle> node(String name) {
        if (localNode.name().equals(name)) return Optional.of(localNode);
        if (members.contains(name)) return Optional.of(remoteHandles.computeIfAbsent(name, RemoteNodeHandle::new));
        return Optional.empty();
    }

    public NodeHandle nodeOf(ICloudServer server) {
        String name = server instanceof CloudServer cs ? cs.nodeName() : null;
        if (name == null || name.equals(localNode.name())) return localNode;
        return remoteHandles.computeIfAbsent(name, RemoteNodeHandle::new);
    }

    /**
     * The node a new server will be started on (head only): the reachable node with the fewest servers.
     * Proper selection (memory, software, allowed nodes, strategies) follows with the NodeSelector.
     */
    public NodeHandle selectNodeForNewServer(ITemplate template) {
        if (!clustered) return localNode;
        long now = System.currentTimeMillis();
        var servers = PocketCloud.instance().servers();
        String best = nodeName();
        long bestCount = servers.localServers().size();
        for (Peer peer : peers.values()) {
            if (!peer.connected() || !peer.alive(now)) continue;
            if (!NodeState.FOLLOWER.name().equals(peer.state()) && !NodeState.HEAD.name().equals(peer.state())) continue;
            long count = servers.serversOfNode(peer.name()).size();
            if (count < bestCount || (count == bestCount && peer.name().compareTo(best) < 0)) {
                best = peer.name();
                bestCount = count;
            }
        }
        return best.equals(nodeName()) ? localNode : remoteHandles.computeIfAbsent(best, RemoteNodeHandle::new);
    }

    public Collection<NodeHandle> nodes() {
        List<NodeHandle> all = new ArrayList<>();
        all.add(localNode);
        for (ClusterMembers.Member m : members.all()) {
            if (!m.name().equals(nodeName())) all.add(remoteHandles.computeIfAbsent(m.name(), RemoteNodeHandle::new));
        }
        return all;
    }
}
