package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.component.server.ICloudServer;
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

    private final Path directory = PocketCloudPaths.storage().internal().with("cluster").asPath();
    @Getter
    private final ClusterCertificates certificates = new ClusterCertificates(directory);
    @Getter
    private final ClusterMembers members = new ClusterMembers(directory.resolve("members.json"));
    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private final Set<String> dialing = ConcurrentHashMap.newKeySet();

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
            }
            default -> {
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
            if (!wasAlive) CloudLogger.get().info("Node §b{} §8(§b{}:{}§8) §ris §aconnected§r.", id.name(), id.address(), id.port());
        }

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

    private void respond(Channel channel, RequestPacket request, ResponsePacket response) {
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

    // ------------------------------------------------------------------ node handles

    public LocalNodeHandle localNode() {
        return localNode;
    }

    public Optional<NodeHandle> node(String name) {
        if (localNode.name().equals(name)) return Optional.of(localNode);
        return Optional.empty();
    }

    public NodeHandle nodeOf(ICloudServer server) {
        return localNode;
    }

    /**
     * The node a new server will be started on.
     */
    public NodeHandle selectNodeForNewServer() {
        return localNode;
    }

    public Collection<NodeHandle> nodes() {
        return List.of(localNode);
    }
}
