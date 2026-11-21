package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.component.server.ICloudServer;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Knows all nodes of the cluster. For now that is only the local node,
 * so every server runs on it.
 */
@Getter
@Accessors(fluent = true)
public final class ClusterManager {

    private final LocalNodeHandle localNode = new LocalNodeHandle(LocalNodeHandle.DEFAULT_NAME);

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
