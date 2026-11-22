package de.pocketcloud.cloud.cluster.scheduling;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.component.software.IServerSoftware;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.ClusterModule;
import de.pocketcloud.cloud.cluster.Peer;
import de.pocketcloud.cloud.cluster.scheduling.packet.HeadDrainResultPacket;
import de.pocketcloud.cloud.cluster.scheduling.packet.NodeDrainRequestPacket;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.network.packet.CloudPacket;
import de.pocketcloud.shared.component.software.ServerSoftware;
import io.netty.channel.Channel;

import java.nio.file.Files;
import java.util.*;

/**
 * Scheduling state that is not part of the node protocol itself (cluster spec, "Scheduling"):
 * <ul>
 *     <li>Drain mode: a draining node gets no new servers, its running servers keep running.
 *     Only the head changes the drain set ({@code node drain}/{@code node undrain} on other nodes is forwarded);
 *     the set travels in every heartbeat and is persisted on every node ({@link DrainState}).</li>
 *     <li>The software a node can run: every node reports the names of its installed server software
 *     (registered, enabled, jar present) in its heartbeat.</li>
 * </ul>
 * The {@link de.pocketcloud.cloud.cluster.NodeSelector} uses both.
 */
public final class SchedulingModule implements ClusterModule {

    public static final String STAT_DRAIN = "drain";
    public static final String STAT_DRAIN_STATE = "drainState";
    public static final String STAT_SOFTWARES = "softwares";

    private final DrainState drain = new DrainState(PocketCloudPaths.storage().internal().with("cluster").with("drain.json").asPath());
    private ClusterManager cluster;
    private volatile boolean running;

    public static Optional<SchedulingModule> get() {
        ClusterManager cluster = PocketCloud.instance().cluster();
        if (!cluster.clustered()) return Optional.empty();
        for (ClusterModule module : cluster.modules()) {
            if (module instanceof SchedulingModule m && m.running) return Optional.of(m);
        }
        return Optional.empty();
    }

    @Override
    public Collection<Class<? extends Packet>> packets() {
        return List.of(NodeDrainRequestPacket.class, HeadDrainResultPacket.class);
    }

    @Override
    public void start(ClusterManager cluster) {
        this.cluster = cluster;
        drain.load();
        running = true;
        if (drain.contains(cluster.nodeName())) {
            CloudLogger.get().warn("This node is in §edrain mode§r: it gets no new servers until §bnode undrain {}§r.", cluster.nodeName());
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    // ------------------------------------------------------------------ heartbeat

    @Override
    public void heartbeatStats(Map<String, Object> stats) {
        if (!running) return;
        stats.put(STAT_DRAIN, drain.contains(cluster.nodeName()));
        stats.put(STAT_DRAIN_STATE, drain.write());
        stats.put(STAT_SOFTWARES, localSoftwares());
    }

    @Override
    public void onHeartbeat(String from, Map<String, Object> stats) {
        if (!running || stats == null) return;
        Set<String> before = drain.nodes();
        if (drain.adoptIfNewer(stats.get(STAT_DRAIN_STATE))) {
            Set<String> after = drain.nodes();
            for (String node : after) if (!before.contains(node)) CloudLogger.get().info("Node §b{} §ris now in §edrain mode§r.", node);
            for (String node : before) if (!after.contains(node)) CloudLogger.get().info("Node §b{} §ris §ano longer draining§r.", node);
        }
    }

    // ------------------------------------------------------------------ drain

    public boolean draining(String node) {
        return drain.contains(node);
    }

    public Set<String> drainingNodes() {
        return drain.nodes();
    }

    /**
     * Sets or clears drain mode of a node. Runs on the head; other nodes forward the request.
     *
     * @param stopServers also stop the non-static servers of the node, so they are replaced on other nodes
     * @return the message for the user
     */
    public Promise<String> setDrain(String node, boolean draining, boolean stopServers) {
        if (cluster.isHead()) {
            try {
                return Promise.resolved(applyDrain(node, draining, stopServers, cluster.nodeName()));
            } catch (IllegalArgumentException e) {
                return Promise.rejected(e);
            }
        }

        String head = cluster.election().head();
        if (head == null || head.isEmpty()) return Promise.rejected(new IllegalStateException("There is no head right now."));
        Promise<String> promise = new Promise<>();
        NodeDrainRequestPacket request = new NodeDrainRequestPacket(node, draining, stopServers);
        request.then(response -> {
            if (response instanceof HeadDrainResultPacket r && r.isSuccess()) promise.resolve(r.getMessage() + " §8(§rvia head §b" + head + "§8)");
            else if (response instanceof HeadDrainResultPacket r) promise.reject(new IllegalArgumentException(r.getMessage()));
            else promise.reject(new IllegalStateException("unexpected response"));
        }).failure((_, ex, reason) -> promise.reject(ex != null ? ex : new IllegalStateException(String.valueOf(reason))));
        if (!cluster.send(head, request)) return Promise.rejected(new IllegalStateException("The head " + head + " is not connected."));
        return promise;
    }

    private String applyDrain(String node, boolean draining, boolean stopServers, String requestedBy) {
        if (!cluster.members().contains(node)) throw new IllegalArgumentException("The node " + node + " is not a member of this cluster.");
        boolean changed = drain.set(node, draining);
        String result;
        if (draining) {
            result = changed ? "Node §b" + node + " §ris now in §edrain mode§r: it gets no new servers, running servers keep running."
                    : "Node §b" + node + " §ris already in drain mode.";
        } else {
            result = changed ? "Node §b" + node + " §ris §ano longer draining §rand gets new servers again." : "Node §b" + node + " §rwas not draining.";
        }
        if (changed) CloudLogger.get().info("{} §8(§rrequested on §b{}§8)", result, requestedBy);

        if (draining && stopServers) {
            List<CloudServer> stop = new ArrayList<>();
            Collection<CloudServer> servers = node.equals(cluster.nodeName())
                    ? PocketCloud.instance().servers().localServers()
                    : PocketCloud.instance().servers().serversOfNode(node);
            for (CloudServer server : servers) {
                if (server.template().settings().staticServers()) continue;
                if (server.status().isStopping() || server.status().isOffline()) continue;
                stop.add(server);
            }
            stop.forEach(CloudServer::stop);
            String names = String.join("§8, §b", stop.stream().map(CloudServer::name).toList());
            result += " Stopping §b" + stop.size() + " §rserver(s)" + (stop.isEmpty() ? "" : "§8: §b" + names) + "§r; they are replaced on other nodes.";
            if (!stop.isEmpty()) CloudLogger.get().info("Drain of §b{}§r: stopping §b{}§r.", node, names);
        }
        return result;
    }

    // ------------------------------------------------------------------ software

    /** Names of the server software this node can run right now. */
    public static List<String> localSoftwares() {
        List<String> names = new ArrayList<>();
        var manager = PocketCloud.instance().softwares();
        if (manager == null) return names;
        for (IServerSoftware software : manager.getAll()) {
            if (!(software instanceof ServerSoftware s) || manager.disabled(s)) continue;
            try {
                if (Files.isRegularFile(PocketCloud.instance().software().sourceFile(s).toPath())) names.add(s.name());
            } catch (RuntimeException ignored) {
                // software without download info
            }
        }
        return names;
    }

    /**
     * Software reported by a node, or null if it did not report any (older node: treated as "has everything").
     */
    public Collection<String> softwaresOf(String node) {
        if (node.equals(cluster.nodeName())) return localSoftwares();
        Peer peer = cluster.peer(node).orElse(null);
        if (peer == null) return null;
        Object raw = peer.stats().get(STAT_SOFTWARES);
        if (!(raw instanceof Collection<?> list)) return null;
        return list.stream().map(String::valueOf).toList();
    }

    // ------------------------------------------------------------------ packets

    @Override
    public boolean onPacket(String from, Channel channel, CloudPacket packet) {
        if (!(packet instanceof NodeDrainRequestPacket request)) return false;
        if (!cluster.isHead()) {
            cluster.respond(channel, request, new HeadDrainResultPacket(false, "This node is not the head (anymore)."));
            return true;
        }
        try {
            cluster.respond(channel, request, new HeadDrainResultPacket(true, applyDrain(request.getNode(), request.isDrain(), request.isStopServers(), from)));
        } catch (IllegalArgumentException e) {
            cluster.respond(channel, request, new HeadDrainResultPacket(false, e.getMessage()));
        }
        return true;
    }
}
