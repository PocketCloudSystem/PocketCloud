package de.pocketcloud.cloud.http.route.v1.node;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.ClusterManager;
import de.pocketcloud.cloud.cluster.ClusterMembers;
import de.pocketcloud.cloud.cluster.NodeSelector;
import de.pocketcloud.cloud.cluster.Peer;
import de.pocketcloud.cloud.cluster.scheduling.SchedulingModule;
import de.pocketcloud.cloud.http.annotation.ApiVersion;
import de.pocketcloud.cloud.http.annotation.GetRoute;
import de.pocketcloud.cloud.http.annotation.PathVariable;
import de.pocketcloud.cloud.http.annotation.PostRoute;
import de.pocketcloud.cloud.http.exception.HttpException;
import de.pocketcloud.cloud.http.io.HttpRequest;
import de.pocketcloud.cloud.http.io.HttpResponse;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.util.VersionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The nodes of the cluster, as seen by the node that answers. Without a cluster there is exactly one node.
 */
@ApiVersion(1)
public final class NodeRoutes {

    @GetRoute("/nodes")
    public void list(HttpRequest request, HttpResponse response) {
        ClusterManager cluster = PocketCloud.instance().cluster();
        JsonObject result = new JsonObject();
        result.addProperty("clustered", cluster.clustered());
        result.addProperty("head", cluster.election().head());
        result.addProperty("term", cluster.election().term());
        JsonArray nodes = new JsonArray();
        for (String name : names()) nodes.add(node(name));
        result.add("nodes", nodes);
        response.json(result);
    }

    @GetRoute("/nodes/{name}")
    public void info(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        if (!names().contains(name)) throw new HttpException(404, "Node not found.");
        JsonObject json = node(name);
        JsonArray servers = new JsonArray();
        for (CloudServer server : serversOf(name)) servers.add(server.name());
        json.add("serverNames", servers);
        response.json(json);
    }

    /**
     * Puts the node into drain mode: it gets no new servers. {@code ?stop=true} (or {@code {"stop": true}})
     * also stops its non-static servers so they are replaced on other nodes. Processed by the head.
     */
    @PostRoute("/nodes/{name}/drain")
    public void drain(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        boolean stop = Boolean.parseBoolean(request.queryParam("stop", "false"));
        if (!stop && request.hasBody()) {
            try {
                var body = request.json();
                stop = body != null && body.has("stop") && body.get("stop").getAsBoolean();
            } catch (RuntimeException ignored) {
                // no JSON body
            }
        }
        setDrain(response, name, true, stop);
    }

    @PostRoute("/nodes/{name}/undrain")
    public void undrain(HttpRequest request, HttpResponse response, @PathVariable("name") String name) {
        setDrain(response, name, false, false);
    }

    private void setDrain(HttpResponse response, String name, boolean drain, boolean stop) {
        SchedulingModule module = SchedulingModule.get().orElseThrow(() -> new HttpException(409, "Drain mode only exists in a cluster."));
        if (!names().contains(name)) throw new HttpException(404, "Node not found.");
        String message;
        try {
            message = module.setDrain(name, drain, stop).get();
        } catch (Throwable e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new HttpException(409, String.valueOf(cause.getMessage()));
        }
        JsonObject json = node(name);
        json.addProperty("message", message.replaceAll("§.", ""));
        response.json(json);
    }

    private List<String> names() {
        ClusterManager cluster = PocketCloud.instance().cluster();
        List<String> names = new ArrayList<>();
        names.add(cluster.nodeName());
        if (cluster.clustered()) {
            for (ClusterMembers.Member member : cluster.members().all()) {
                if (!names.contains(member.name())) names.add(member.name());
            }
        }
        return names;
    }

    private static List<CloudServer> serversOf(String node) {
        var servers = PocketCloud.instance().servers();
        if (node.equals(PocketCloud.instance().cluster().nodeName())) return List.copyOf(servers.localServers());
        return List.copyOf(servers.serversOfNode(node));
    }

    private JsonObject node(String name) {
        ClusterManager cluster = PocketCloud.instance().cluster();
        long now = System.currentTimeMillis();
        boolean self = name.equals(cluster.nodeName());
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("local", self);
        json.addProperty("head", name.equals(cluster.election().head()));

        ClusterMembers.Member member = cluster.members().get(name).orElse(null);
        if (member != null) {
            json.addProperty("address", member.address());
            json.addProperty("port", member.port());
        } else if (self) {
            json.addProperty("address", PocketCloud.instance().nodeConfig().advertisedAddress());
            json.addProperty("port", PocketCloud.instance().nodeConfig().clusterPort());
        }

        SchedulingModule scheduling = SchedulingModule.get().orElse(null);
        json.addProperty("drain", scheduling != null && scheduling.draining(name));
        if (scheduling != null) {
            var softwares = scheduling.softwaresOf(name);
            if (softwares != null) {
                JsonArray list = new JsonArray();
                softwares.forEach(list::add);
                json.add("softwares", list);
            }
        }
        json.addProperty("cpuLimit", NodeSelector.CPU_LIMIT);

        List<CloudServer> servers = serversOf(name);
        json.addProperty("servers", servers.size());
        json.addProperty("usedMemoryMb", servers.stream().mapToInt(s -> s.template().settings().maxMemory()).sum());

        if (self) {
            json.addProperty("state", cluster.election().state().name());
            json.addProperty("health", "ALIVE");
            json.addProperty("version", VersionInfo.VERSION);
            json.addProperty("maxMemoryMb", PocketCloud.instance().nodeConfig().maxMemoryMb());
            json.addProperty("startedAt", cluster.election().startedAt());
            json.addProperty("cpu", PocketCloud.instance().performanceStats().systemCpuUsage());
            return json;
        }

        Peer peer = cluster.peer(name).orElse(null);
        if (peer == null) {
            json.addProperty("state", "UNKNOWN");
            json.addProperty("health", "DEAD");
            return json;
        }

        json.addProperty("state", peer.state());
        json.addProperty("health", peer.health(now));
        json.addProperty("connected", peer.connected());
        json.addProperty("lastSeen", peer.lastSeen());
        if (peer.identity() != null) {
            json.addProperty("version", peer.identity().version());
            json.addProperty("startedAt", peer.identity().startedAt());
        }
        Map<String, Object> stats = peer.stats();
        Object maxMemory = stats.get("maxMemoryMb");
        if (maxMemory instanceof Number n) json.addProperty("maxMemoryMb", n.longValue());
        Object cpu = stats.get("cpu");
        if (cpu instanceof Number n) json.addProperty("cpu", n.doubleValue());
        return json;
    }
}
