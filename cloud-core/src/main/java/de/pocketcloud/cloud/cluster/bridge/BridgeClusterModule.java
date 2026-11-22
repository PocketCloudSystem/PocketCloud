package de.pocketcloud.cloud.cluster.bridge;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.ClusterModule;
import de.pocketcloud.cloud.cluster.ClusterNodeProvider;
import de.pocketcloud.cloud.cluster.Peer;
import de.pocketcloud.cloud.cluster.bridge.packet.*;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.RequestPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.shared.network.packet.type.ServerCommandExecutionResult;
import io.netty.channel.Channel;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Everything bridges, proxies and the HTTP API need across nodes:
 * <ul>
 *     <li>bridge packets for a server of another node are relayed to that node ({@link #relay})</li>
 *     <li>commands, logs, saves of a server of another node are requested from that node ({@link #requestAction})</li>
 *     <li>players are shared between all nodes ({@link #publishPlayer})</li>
 *     <li>HTTP API requests are forwarded to the head or the owning node ({@link ClusterHttpForwarder})</li>
 * </ul>
 */
public final class BridgeClusterModule implements ClusterModule {

    /** Packets are limited to 1 MB: keep text payloads well below. */
    public static final int MAX_TEXT_CHARS = 600_000;
    public static final int MAX_LOG_LINES = 5_000;

    private final ExecutorService executor = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "PocketCloud-ClusterBridge");
        t.setDaemon(true);
        return t;
    });

    private volatile ClusterManager cluster;
    private final ClusterHttpForwarder http = new ClusterHttpForwarder(this);

    public static Optional<BridgeClusterModule> get() {
        ClusterManager cluster = PocketCloud.instance().cluster();
        if (cluster == null || !cluster.clustered()) return Optional.empty();
        for (ClusterModule module : cluster.modules()) {
            if (module instanceof BridgeClusterModule m && m.cluster != null) return Optional.of(m);
        }
        return Optional.empty();
    }

    @Override
    public Collection<Class<? extends Packet>> packets() {
        return List.of(NodeBridgeRelayPacket.class, NodeServerActionPacket.class, NodeServerActionResultPacket.class,
                NodePlayerStatePacket.class, NodeHttpForwardPacket.class, NodeHttpForwardResultPacket.class);
    }

    @Override
    public void start(ClusterManager cluster) {
        this.cluster = cluster;
        cluster.scheduler().scheduleAtFixedRate(this::syncNodesIfChanged, 2, 2, TimeUnit.SECONDS);
    }

    private volatile String lastNodes = "";

    /** Sends the nodes to the local bridges whenever a node joins, leaves, dies or the head changes. */
    private void syncNodesIfChanged() {
        try {
            ClusterNodeProvider provider = new ClusterNodeProvider(cluster);
            String current = provider.snapshot().toString();
            if (current.equals(lastNodes)) return;
            lastNodes = current;
            provider.buildSyncPacket().broadcast();
            CloudLogger.get().debug("Synced nodes to the bridges: {}", current);
        } catch (Exception e) {
            CloudLogger.get().debug("Failed to sync the nodes: {}", e.getMessage());
        }
    }

    @Override
    public void stop() {
        cluster = null;
        executor.shutdownNow();
    }

    public ClusterManager cluster() {
        return cluster;
    }

    ExecutorService executor() {
        return executor;
    }

    public ClusterHttpForwarder http() {
        return http;
    }

    @Override
    public boolean onPacket(String from, Channel channel, CloudPacket packet) {
        switch (packet) {
            case NodeBridgeRelayPacket relay -> handleRelay(from, relay);
            case NodeServerActionPacket action -> executor.execute(() -> handleAction(from, channel, action));
            case NodePlayerStatePacket state -> handlePlayerState(from, state);
            case NodeHttpForwardPacket forward -> http.handleForwarded(from, channel, forward);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onPeerConnected(String name) {
        List<Map<String, Object>> owned = new ArrayList<>();
        for (CloudPlayer player : PocketCloud.instance().players().ownedPlayers()) owned.add(player.write());
        cluster.send(name, new NodePlayerStatePacket(true, owned, List.of()));
    }

    @Override
    public void onPeerGone(String name) {
        int removed = PocketCloud.instance().players().removePlayersOfNode(name);
        if (removed > 0) CloudLogger.get().debug("Removed {} player(s) of the proxies of node {}", removed, name);
    }

    // ------------------------------------------------------------------ bridge packet relay

    /**
     * Sends a bridge packet to a server of another node: the owning node hands it to the server's bridge.
     */
    public CompletableFuture<Void> relay(CloudServer server, ClientboundPacket packet) {
        if (!(packet instanceof CloudPacket cloudPacket) || packet instanceof RequestPacket) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Cannot relay " + packet.getClass().getSimpleName() + " to another node"));
        }

        PacketData data = new PacketData();
        cloudPacket.encode(data);
        String payload = Base64.getEncoder().encodeToString(data.toByteArray());
        if (!cluster.send(server.nodeName(), new NodeBridgeRelayPacket(server.name(), cloudPacket.getName(), payload))) {
            return CompletableFuture.failedFuture(new IllegalStateException("Node " + server.nodeName() + " is not connected"));
        }

        CloudLogger.get().debug("Relayed {} for {} to node {}", cloudPacket.getName(), server.name(), server.nodeName());
        return CompletableFuture.completedFuture(null);
    }

    private void handleRelay(String from, NodeBridgeRelayPacket relay) {
        CloudServer server = localServer(relay.getServerName());
        if (server == null) {
            CloudLogger.get().debug("Node {} relayed {} for unknown server {}", from, relay.getPacketName(), relay.getServerName());
            return;
        }

        Packet packet = PocketCloud.instance().packets().get(relay.getPacketName());
        if (!(packet instanceof ClientboundPacket clientbound)) {
            CloudLogger.get().warn("Node §b{} §rsent a message this node does not know. Do all nodes run the same version?", from);
            CloudLogger.get().debug("Unknown relayed bridge packet {} from {}", relay.getPacketName(), from);
            return;
        }

        packet.decode(PacketData.fromBytes(Base64.getDecoder().decode(relay.getPayload())));
        CloudLogger.get().debug("Delivering {} from node {} to {}", relay.getPacketName(), from, server.name());
        server.sendPacket(clientbound);
    }

    // ------------------------------------------------------------------ server actions

    public Promise<NodeServerActionResultPacket> requestAction(CloudServer server, NodeServerActionPacket.Action action, String argument, Map<String, Object> data) {
        Promise<NodeServerActionResultPacket> promise = new Promise<>();
        String node = server.nodeName();
        Peer peer = cluster.peer(node).orElse(null);
        if (peer == null || !peer.connected()) {
            promise.reject(new IllegalStateException("node " + node + " is not connected"));
            return promise;
        }

        NodeServerActionPacket request = new NodeServerActionPacket(server.name(), action, argument, data);
        request.then(response -> {
            if (response instanceof NodeServerActionResultPacket result) promise.resolve(result);
            else promise.reject(new IllegalStateException("unexpected response " + response.getClass().getSimpleName()));
        }).failure((_, e, reason) -> promise.reject(e != null ? e : new IllegalStateException("request to node " + node + " failed: " + reason)));
        if (!cluster.send(node, request)) promise.reject(new IllegalStateException("node " + node + " is not connected"));
        return promise;
    }

    public Promise<ServerCommandExecutionResult> dispatch(CloudServer server, String commandLine) {
        return requestAction(server, NodeServerActionPacket.Action.COMMAND, commandLine, null).thenApply(result -> {
            if (!result.isSuccess()) throw new IllegalStateException(result.getError());
            return ServerCommandExecutionResult.read(result.getData());
        });
    }

    public Promise<Void> save(CloudServer server) {
        return requestAction(server, NodeServerActionPacket.Action.SAVE, null, null).thenApply(result -> {
            if (!result.isSuccess()) throw new IllegalStateException(result.getError());
            return null;
        });
    }

    /**
     * Log lines of a server of another node, starting at {@code fromLine}. Blocks up to 5 seconds.
     */
    public Optional<List<String>> readLogs(CloudServer server, int fromLine) {
        try {
            NodeServerActionResultPacket result = requestAction(server, NodeServerActionPacket.Action.LOGS, String.valueOf(fromLine), null)
                    .toCompletableFuture().get(5, TimeUnit.SECONDS);
            return result.isSuccess() ? Optional.ofNullable(result.getLines()) : Optional.empty();
        } catch (Exception e) {
            CloudLogger.get().debug("Failed to read the logs of {} from node {}: {}", server.name(), server.nodeName(), e.getMessage());
            return Optional.empty();
        }
    }

    public Promise<List<String>> readLogsAsync(CloudServer server, int fromLine) {
        return requestAction(server, NodeServerActionPacket.Action.LOGS, String.valueOf(fromLine), null).thenApply(result -> {
            if (!result.isSuccess()) throw new IllegalStateException(result.getError());
            return result.getLines() == null ? List.of() : result.getLines();
        });
    }

    private void handleAction(String from, Channel channel, NodeServerActionPacket request) {
        CloudServer server = localServer(request.getServerName());
        if (server == null) {
            cluster.respond(channel, request, NodeServerActionResultPacket.failed("server " + request.getServerName() + " does not run on node " + cluster.nodeName()));
            return;
        }

        try {
            switch (request.getAction()) {
                case COMMAND -> {
                    CloudLogger.get().info("Executing §b{} §ron §b{} §8(§rfrom §b{}§8)§r.", request.getArgument(), server.name(), from);
                    server.dispatch(request.getArgument())
                            .thenSuccess(result -> cluster.respond(channel, request, NodeServerActionResultPacket.ok(null, result.write())))
                            .failure(e -> cluster.respond(channel, request, NodeServerActionResultPacket.failed(String.valueOf(e.getMessage()))));
                }
                case LOGS -> {
                    List<String> lines = server.retrieveLogs();
                    if (lines == null) {
                        cluster.respond(channel, request, NodeServerActionResultPacket.failed("no log file"));
                        return;
                    }
                    int from0 = parseInt(request.getArgument());
                    if (from0 < 0) from0 = Math.max(0, lines.size() + from0);
                    cluster.respond(channel, request, NodeServerActionResultPacket.ok(limit(lines, from0), Map.of("total", lines.size())));
                }
                case SAVE -> PocketCloud.instance().servers().save(server)
                        .thenSuccess(_ -> cluster.respond(channel, request, NodeServerActionResultPacket.ok(null, null)))
                        .failure(e -> cluster.respond(channel, request, NodeServerActionResultPacket.failed(String.valueOf(e.getMessage()))));
                case STATUS -> {
                    server.status(ServerStatus.valueOf(request.getArgument()));
                    cluster.respond(channel, request, NodeServerActionResultPacket.ok(null, null));
                }
                case STORAGE -> {
                    if (request.getData() != null) server.storage().syncIn(request.getData());
                    cluster.respond(channel, request, NodeServerActionResultPacket.ok(null, null));
                }
            }
        } catch (Exception e) {
            cluster.respond(channel, request, NodeServerActionResultPacket.failed(e.getClass().getSimpleName() + ": " + e.getMessage()));
        }
    }

    /** Lines from {@code from}, capped by line count and size (the newest lines are kept). */
    private static List<String> limit(List<String> lines, int from) {
        if (from >= lines.size()) return List.of();
        List<String> part = lines.subList(from, lines.size());
        if (part.size() > MAX_LOG_LINES) part = part.subList(part.size() - MAX_LOG_LINES, part.size());
        int chars = 0;
        int start = part.size();
        while (start > 0 && chars + part.get(start - 1).length() + 1 <= MAX_TEXT_CHARS) {
            chars += part.get(start - 1).length() + 1;
            start--;
        }
        return new ArrayList<>(part.subList(start, part.size()));
    }

    private static int parseInt(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static CloudServer localServer(String name) {
        ICloudServer server = PocketCloud.instance().servers().get(name).orElse(null);
        return server instanceof CloudServer cs && !cs.remote() ? cs : null;
    }

    // ------------------------------------------------------------------ players

    /** Called whenever a player changes on this node. */
    public void publishPlayer(CloudPlayer player, boolean removed) {
        ClusterManager c = cluster;
        if (c == null) return;
        Map<String, Object> document = removed ? null : player.write();
        c.broadcast(() -> removed
                ? new NodePlayerStatePacket(false, List.of(), List.of(player.name()))
                : new NodePlayerStatePacket(false, List.of(document), List.of()));
    }

    private void handlePlayerState(String from, NodePlayerStatePacket packet) {
        var players = PocketCloud.instance().players();
        for (Map<String, Object> document : packet.getPlayers()) {
            try {
                players.applyRemote(CloudPlayer.read(document), from);
            } catch (Exception e) {
                CloudLogger.get().exception("Invalid player state from node {}", e, from);
            }
        }

        for (String name : packet.getRemoved()) players.removeRemote(name, from);
    }
}
