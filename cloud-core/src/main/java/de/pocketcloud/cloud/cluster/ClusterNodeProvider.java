package de.pocketcloud.cloud.cluster;

import de.pocketcloud.api.cluster.INode;
import de.pocketcloud.api.cluster.INodeProvider;
import de.pocketcloud.network.packet.impl.SyncPacket;
import de.pocketcloud.shared.cluster.BaseNode;
import de.pocketcloud.shared.sync.SyncType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * {@code CloudAPI.instance().nodes()} on the cloud side: the members of the cluster, or this node alone.
 */
public final class ClusterNodeProvider implements INodeProvider {

    private final ClusterManager cluster;

    public ClusterNodeProvider(ClusterManager cluster) {
        this.cluster = cluster;
    }

    @Override
    public Collection<INode> getAll() {
        if (!cluster.clustered()) return List.of(cluster.localNode());
        return List.copyOf(cluster.nodes());
    }

    @Override
    public Optional<INode> get(String name) {
        if (name == null) return Optional.empty();
        return cluster.node(name).map(n -> (INode) n);
    }

    /** Snapshot of all nodes for the bridges. */
    public List<BaseNode> snapshot() {
        return getAll().stream().map(BaseNode::of).toList();
    }

    public SyncPacket buildSyncPacket() {
        List<Object> nodes = snapshot().stream().map(n -> (Object) n.write()).toList();
        return SyncPacket.create(SyncType.NODES, data -> data.write(nodes));
    }
}
