package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.cloud.cluster.state.ClusterStateStore;
import de.pocketcloud.cloud.cluster.state.LocalStateStore;
import de.pocketcloud.cloud.cluster.storage.LocalTemplateStorage;
import de.pocketcloud.cloud.cluster.storage.TemplateStorage;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Knows all nodes of the cluster. For now that is only the local node,
 * so every server runs on it.
 * <p>
 * Also owns the shared {@link ClusterStateStore} and the {@link TemplateStorage}.
 */
@Getter
@Accessors(fluent = true)
public final class ClusterManager {

    private final LocalNodeHandle localNode = new LocalNodeHandle(LocalNodeHandle.DEFAULT_NAME);
    private final ClusterStateStore state = new LocalStateStore();
    private final TemplateStorage templateStorage = new LocalTemplateStorage();

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
