package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.CloudServer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Picks the node for a new server (head only).
 * <p>
 * Filters (all must match):
 * <ul>
 *     <li>the node is connected and FOLLOWER or HEAD (not suspect, not joining)</li>
 *     <li>{@code template.nodes} is empty or contains the node; in a cluster PROXY templates must name their nodes</li>
 *     <li>free memory budget: {@code maxMemoryMb} of the node (0 = unlimited) minus the {@code maxMemory} of its servers
 *     is at least {@code template.maxMemory}</li>
 * </ul>
 * Nodes with a CPU load of 85 % or more (heartbeat) are only used when no other node fits; the node itself
 * also delays starts while its CPU is busy.
 * <p>
 * Sorting by {@code cluster.strategy} in config.yml: {@code least-memory} (default), {@code fill}, {@code round-robin}.
 */
public final class NodeSelector {

    public static final double CPU_LIMIT = 85;

    private final ClusterManager cluster;
    private final AtomicInteger roundRobin = new AtomicInteger();

    public NodeSelector(ClusterManager cluster) {
        this.cluster = cluster;
    }

    record Candidate(String name, long freeMemoryMb, int servers, double cpu) {
    }

    /**
     * @return the node, or null if no node may run a server of the template right now
     */
    public NodeHandle select(ITemplate template) {
        // A single node runs everything, whatever the template says (unchanged single-node behaviour).
        if (!cluster.clustered()) return cluster.localNode();

        if (template.templateType().isProxy() && template.settings().nodes().isEmpty()) {
            if (cluster.throttle("proxy-without-nodes:" + template.name())) {
                CloudLogger.get().warn("The proxy template §b{} §rhas no §bnodes §rset and is §cdisabled §rin the cluster. " +
                        "Use §btemplate nodes {} <node,...> §rto choose the nodes for its proxies.", template.name(), template.name());
            }
            return null;
        }

        List<Candidate> candidates = candidates(template);
        if (candidates.isEmpty()) return nodeOrWarn(template, null);

        List<Candidate> idle = candidates.stream().filter(c -> c.cpu() < CPU_LIMIT).toList();
        if (!idle.isEmpty()) candidates = idle;

        Candidate chosen = switch (strategy()) {
            case "fill" -> candidates.stream()
                    .min(Comparator.comparingLong(Candidate::freeMemoryMb).thenComparingInt(Candidate::servers).thenComparing(Candidate::name))
                    .orElseThrow();
            case "round-robin" -> {
                List<Candidate> sorted = candidates.stream().sorted(Comparator.comparing(Candidate::name)).toList();
                yield sorted.get(Math.floorMod(roundRobin.getAndIncrement(), sorted.size()));
            }
            default -> candidates.stream()
                    .min(Comparator.comparingLong((Candidate c) -> -c.freeMemoryMb()).thenComparingInt(Candidate::servers).thenComparing(Candidate::name))
                    .orElseThrow();
        };

        return cluster.node(chosen.name()).orElse(null);
    }

    private NodeHandle nodeOrWarn(ITemplate template, NodeHandle node) {
        if (node == null && cluster.throttle("no-node:" + template.name())) {
            List<String> allowed = template.settings().nodes();
            CloudLogger.get().warn("No node can start a server of §b{} §rright now §8(§rallowed nodes§8: §b{}§8, §rneeds §b{} MB§8)§r.",
                    template.name(), allowed.isEmpty() ? "all" : String.join("§8, §b", allowed), template.settings().maxMemory());
        }
        return node;
    }

    private List<Candidate> candidates(ITemplate template) {
        long now = System.currentTimeMillis();
        List<Candidate> result = new ArrayList<>();
        int needed = Math.max(0, template.settings().maxMemory());

        String self = cluster.nodeName();
        if (template.settings().allowsNode(self)) {
            long max = PocketCloud.instance().nodeConfig().maxMemoryMb();
            Collection<CloudServer> servers = PocketCloud.instance().servers().localServers();
            long free = free(max, servers);
            if (free >= needed) {
                result.add(new Candidate(self, free, servers.size(), PocketCloud.instance().performanceStats().systemCpuUsage()));
            }
        }

        for (Peer peer : cluster.peers()) {
            if (!peer.connected() || !"ALIVE".equals(peer.health(now))) continue;
            if (!NodeState.FOLLOWER.name().equals(peer.state()) && !NodeState.HEAD.name().equals(peer.state())) continue;
            if (!cluster.members().contains(peer.name()) || !template.settings().allowsNode(peer.name())) continue;

            Map<String, Object> stats = peer.stats();
            long max = stats.get("maxMemoryMb") instanceof Number n ? n.longValue() : 0;
            double cpu = stats.get("cpu") instanceof Number n ? n.doubleValue() : 0;
            Collection<CloudServer> servers = PocketCloud.instance().servers().serversOfNode(peer.name());
            long free = free(max, servers);
            if (free < needed) continue;
            result.add(new Candidate(peer.name(), free, servers.size(), cpu));
        }
        return result;
    }

    /** Free memory budget in MB. Without a limit, all nodes count as equally free (Long.MAX_VALUE / 2 minus usage). */
    private static long free(long maxMemoryMb, Collection<CloudServer> servers) {
        long used = 0;
        for (CloudServer server : servers) used += Math.max(0, server.template().settings().maxMemory());
        long budget = maxMemoryMb <= 0 ? Long.MAX_VALUE / 2 : maxMemoryMb;
        return budget - used;
    }

    private static String strategy() {
        return PocketCloud.instance().config().cluster().strategy();
    }
}
