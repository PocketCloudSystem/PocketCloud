package de.pocketcloud.cloud.http.route;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.ClusterMembers;
import de.pocketcloud.cloud.cluster.Peer;
import de.pocketcloud.cloud.http.annotation.GetRoute;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Prometheus text format, the whole cluster as seen by the answering node (same Bearer auth as the API).
 * Node values come from the heartbeats, so they are up to a few seconds old for other nodes.
 */
public final class MetricsRoute {

    @GetRoute("/metrics")
    public void metrics(HttpRequest request, HttpResponse response) {
        StringBuilder out = new StringBuilder();
        ClusterManager cluster = PocketCloud.instance().cluster();
        long now = System.currentTimeMillis();

        Set<String> nodes = new LinkedHashSet<>();
        nodes.add(cluster.nodeName());
        if (cluster.clustered()) for (ClusterMembers.Member m : cluster.members().all()) nodes.add(m.name());
        String head = cluster.clustered() ? cluster.election().head() : cluster.nodeName();

        Map<String, Map<String, Object>> stats = new TreeMap<>();
        for (String node : nodes) {
            Map<String, Object> s = new HashMap<>();
            if (node.equals(cluster.nodeName())) {
                Runtime rt = Runtime.getRuntime();
                s.put("up", 1);
                s.put("usedMemoryMb", (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024);
                s.put("maxMemoryMb", PocketCloud.instance().nodeConfig().maxMemoryMb());
                s.put("cpu", PocketCloud.instance().performanceStats().systemCpuUsage());
            } else {
                Peer peer = cluster.peer(node).orElse(null);
                boolean up = peer != null && peer.alive(now);
                s.put("up", up ? 1 : 0);
                if (up) s.putAll(peer.stats());
            }
            // from the server list, not the heartbeat: its "servers" counts every server the node knows
            s.put("servers", node.equals(cluster.nodeName()) ? PocketCloud.instance().servers().localServers().size() : PocketCloud.instance().servers().serversOfNode(node).size());
            stats.put(node, s);
        }

        gauge(out, "pocketcloud_node_up", "1 if the node is reachable (heartbeat), else 0");
        stats.forEach((node, s) -> sample(out, "pocketcloud_node_up", s.get("up"), "node", node));
        gauge(out, "pocketcloud_node_head", "1 for the head of the cluster");
        stats.keySet().forEach(node -> sample(out, "pocketcloud_node_head", node.equals(head) ? 1 : 0, "node", node));
        gauge(out, "pocketcloud_node_cpu_percent", "System CPU usage of the node's machine");
        stats.forEach((node, s) -> sample(out, "pocketcloud_node_cpu_percent", s.get("cpu"), "node", node));
        gauge(out, "pocketcloud_node_memory_used_megabytes", "Heap used by the cloud process");
        stats.forEach((node, s) -> sample(out, "pocketcloud_node_memory_used_megabytes", s.get("usedMemoryMb"), "node", node));
        gauge(out, "pocketcloud_node_memory_max_megabytes", "maxMemoryMb of the node for its servers (0 = no limit)");
        stats.forEach((node, s) -> sample(out, "pocketcloud_node_memory_max_megabytes", s.get("maxMemoryMb"), "node", node));
        gauge(out, "pocketcloud_node_servers", "Servers running on the node");
        stats.forEach((node, s) -> sample(out, "pocketcloud_node_servers", s.get("servers"), "node", node));

        Map<String, String> templateOf = new HashMap<>();
        Map<String, Integer> serversByStatus = new TreeMap<>();
        for (ICloudServer server : PocketCloud.instance().servers().getAll()) {
            templateOf.put(server.name(), server.templateName());
            serversByStatus.merge(server.templateName() + "\0" + server.status().name(), 1, Integer::sum);
        }
        Map<String, Integer> players = new TreeMap<>();
        for (ITemplate template : PocketCloud.instance().templates().getAll()) players.put(template.name(), 0);
        int playersTotal = 0;
        for (ICloudPlayer player : CloudAPI.instance().players().getAll()) {
            playersTotal++;
            String template = templateOf.get(player.currentServerName());
            if (template != null) players.merge(template, 1, Integer::sum);
        }

        gauge(out, "pocketcloud_servers", "Servers of the cluster by template and status");
        serversByStatus.forEach((key, count) -> {
            String[] parts = key.split("\0", 2);
            sample(out, "pocketcloud_servers", count, "template", parts[0], "status", parts[1]);
        });
        gauge(out, "pocketcloud_players", "Online players by template of their current server");
        players.forEach((template, count) -> sample(out, "pocketcloud_players", count, "template", template));
        gauge(out, "pocketcloud_players_total", "Online players of the cluster");
        sample(out, "pocketcloud_players_total", playersTotal);

        response.bytes("text/plain; version=0.0.4", out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static void gauge(StringBuilder out, String name, String help) {
        out.append("# HELP ").append(name).append(' ').append(help).append('\n');
        out.append("# TYPE ").append(name).append(" gauge\n");
    }

    /** Skips samples without a value (e.g. stats of a node that is down). */
    private static void sample(StringBuilder out, String name, Object value, String... labels) {
        if (!(value instanceof Number number)) return;
        out.append(name);
        if (labels.length > 0) {
            out.append('{');
            for (int i = 0; i < labels.length; i += 2) {
                if (i > 0) out.append(',');
                out.append(labels[i]).append("=\"").append(labels[i + 1].replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            }
            out.append('}');
        }
        out.append(' ').append(number instanceof Double || number instanceof Float ? String.valueOf(number.doubleValue()) : String.valueOf(number.longValue())).append('\n');
    }
}
