package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.redis.RedisConnection;
import de.pocketcloud.cloud.cluster.state.ClusterStateStore;
import de.pocketcloud.cloud.cluster.state.LocalStateStore;
import de.pocketcloud.cloud.cluster.state.RedisStateStore;
import de.pocketcloud.cloud.cluster.state.StateKeys;
import de.pocketcloud.cloud.cluster.storage.LocalTemplateStorage;
import de.pocketcloud.cloud.cluster.storage.TemplateStorage;
import de.pocketcloud.cloud.config.sub.RedisConfiguration;
import de.pocketcloud.cloud.console.log.CloudLogger;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Knows all nodes of the cluster. For now that is only the local node,
 * so every server runs on it.
 * <p>
 * Also owns the shared {@link ClusterStateStore}, the optional {@link RedisConnection}
 * and the {@link TemplateStorage}.
 */
@Getter
@Accessors(fluent = true)
public final class ClusterManager {

    private final LocalNodeHandle localNode = new LocalNodeHandle(LocalNodeHandle.DEFAULT_NAME);
    private final TemplateStorage templateStorage = new LocalTemplateStorage();
    private volatile ClusterStateStore state = new LocalStateStore();
    private volatile RedisConnection redis;

    /**
     * Connects to Redis if it is enabled. Without Redis (or if the connection fails)
     * the in-memory state is used, which is fine for a single machine.
     */
    public void init() {
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

    public void close() {
        RedisConnection connection = redis;
        if (connection == null) return;
        redis = null;
        try {
            connection.close();
        } catch (RuntimeException e) {
            CloudLogger.get().exception("Failed to close the Redis connection", e);
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
        String nodeServersKey = StateKeys.nodeServers(localNode.name());
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
