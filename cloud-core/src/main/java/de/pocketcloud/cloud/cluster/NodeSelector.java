package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.scheduling.SchedulingModule;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.server.CloudServer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Picks the node for a new server (head only), cluster spec "Node-Auswahl".
 * <p>
 * Filters (all must match):
 * <ul>
 *     <li>the node is connected, ALIVE and FOLLOWER or HEAD (not suspect, not joining) and not in drain mode</li>
 *     <li>the node has the software of the template (reported in its heartbeat)</li>
 *     <li>{@code template.nodes} is empty or contains the node; in a cluster PROXY templates must name their nodes</li>
 *     <li>static templates: only {@code template.staticNode} (required in a cluster)</li>
 *     <li>free memory budget: {@code maxMemoryMb} of the node (0 = unlimited) minus the {@code maxMemory} of its servers
 *     is at least {@code template.maxMemory}</li>
 *     <li>CPU load of the node (heartbeat) below {@value #CPU_LIMIT} %</li>
 * </ul>
 * Sorting by {@code strategy} in cluster.yml: {@code least-memory} (default), {@code fill}, {@code round-robin}.
 * If no node fits, the reason per node is logged.
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
        return select(template, Set.of());
    }

    /**
     * @param exclude nodes that must not be used (e.g. a node that just failed to start a server of the template)
     * @return the node, or null if no node may run a server of the template right now
     */
    public NodeHandle select(ITemplate template, Set<String> exclude) {
        // A single node runs everything, whatever the template says (unchanged single-node behaviour).
        if (!cluster.clustered()) return cluster.localNode();

        if (template.templateType().isProxy() && template.settings().nodes().isEmpty()) {
            if (cluster.throttle("proxy-without-nodes:" + template.name())) {
                CloudLogger.get().warn("Proxies of §b{} §rcannot start until you choose their nodes with §btemplate nodes {} <node,...>§r.", template.name(), template.name());
            }
            return null;
        }

        if (template.settings().staticServers() && !template.settings().hasStaticNode()) {
            if (cluster.throttle("static-without-node:" + template.name())) {
                CloudLogger.get().warn("Static servers of §b{} §rcannot start until you choose their node with §btemplate staticnode {} <node>§r.", template.name(), template.name());
            }
            return null;
        }

        Map<String, String> rejected = new LinkedHashMap<>();
        List<Candidate> candidates = candidates(template, exclude, rejected);
        if (candidates.isEmpty()) {
            if (cluster.throttle("no-node:" + template.name())) {
                List<String> reasons = new ArrayList<>();
                rejected.forEach((node, reason) -> reasons.add("§b" + node + "§8: §r" + reason));
                CloudLogger.get().warn("No node can start a server of §b{} §rright now§8: §c{}", template.name(),
                        reasons.isEmpty() ? "no nodes" : String.join("§8; ", reasons));
            }
            return null;
        }

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

        if (!rejected.isEmpty()) {
            CloudLogger.get().debug("Node for {}: {} (skipped: {})", template.name(), chosen.name(), rejected);
        }
        return cluster.node(chosen.name()).orElse(null);
    }

    /**
     * Why a node may not run a new server of the template right now, or null if it may.
     */
    public String reject(ITemplate template, String node) {
        Map<String, String> rejected = new LinkedHashMap<>();
        List<Candidate> list = candidates(template, Set.of(), rejected);
        if (list.stream().anyMatch(c -> c.name().equals(node))) return null;
        return rejected.getOrDefault(node, "not a member");
    }

    private List<Candidate> candidates(ITemplate template, Set<String> exclude, Map<String, String> rejected) {
        long now = System.currentTimeMillis();
        List<Candidate> result = new ArrayList<>();
        int needed = Math.max(0, template.settings().maxMemory());
        SchedulingModule scheduling = SchedulingModule.get().orElse(null);
        String software = template.serverSoftware().name();

        String self = cluster.nodeName();
        List<String> names = new ArrayList<>();
        names.add(self);
        for (ClusterMembers.Member member : cluster.members().all()) if (!names.contains(member.name())) names.add(member.name());

        for (String name : names) {
            boolean local = name.equals(self);
            Peer peer = local ? null : cluster.peer(name).orElse(null);

            String state;
            if (local) {
                state = cluster.election().state().name();
            } else if (peer == null || !peer.connected()) {
                rejected.put(name, "not connected");
                continue;
            } else {
                String health = peer.health(now);
                if (!"ALIVE".equals(health)) {
                    rejected.put(name, "not responding");
                    continue;
                }
                state = peer.state();
            }
            if (!NodeState.FOLLOWER.name().equals(state) && !NodeState.HEAD.name().equals(state)) {
                rejected.put(name, "not ready (" + state.toLowerCase(Locale.ROOT) + ")");
                continue;
            }
            if (exclude.contains(name)) {
                rejected.put(name, "failed to start it just now");
                continue;
            }
            if (scheduling != null && scheduling.draining(name)) {
                rejected.put(name, "draining");
                continue;
            }
            if (template.settings().staticServers() && !name.equals(template.settings().staticNode())) {
                rejected.put(name, "static servers live on " + template.settings().staticNode());
                continue;
            }
            if (!template.settings().allowsNode(name)) {
                rejected.put(name, "not in the template's nodes");
                continue;
            }
            if (!local && de.pocketcloud.cloud.cluster.sync.TemplateReplicator.pausedBehind(peer.stats(), template)) {
                rejected.put(name, "template sync paused there");
                continue;
            }
            Collection<String> softwares = scheduling == null ? null : scheduling.softwaresOf(name);
            if (softwares != null && !softwares.contains(software)) {
                rejected.put(name, "software " + software + " not installed");
                continue;
            }

            long max;
            double cpu;
            Collection<CloudServer> servers;
            if (local) {
                max = PocketCloud.instance().nodeConfig().maxMemoryMb();
                cpu = PocketCloud.instance().performanceStats().systemCpuUsage();
                servers = PocketCloud.instance().servers().localServers();
            } else {
                Map<String, Object> stats = peer.stats();
                max = stats.get("maxMemoryMb") instanceof Number n ? n.longValue() : 0;
                cpu = stats.get("cpu") instanceof Number n ? n.doubleValue() : 0;
                servers = PocketCloud.instance().servers().serversOfNode(name);
            }
            long free = free(max, servers);
            if (free < needed) {
                rejected.put(name, "not enough memory (" + Math.max(0, free) + " MB free, " + needed + " MB needed)");
                continue;
            }
            if (cpu >= CPU_LIMIT) {
                rejected.put(name, String.format(Locale.ROOT, "CPU busy (%.0f %%)", cpu));
                continue;
            }
            result.add(new Candidate(name, free, servers.size(), cpu));
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
