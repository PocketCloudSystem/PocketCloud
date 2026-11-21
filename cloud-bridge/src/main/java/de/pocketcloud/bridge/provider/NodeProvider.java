package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.cluster.INode;
import de.pocketcloud.api.cluster.INodeProvider;
import de.pocketcloud.shared.cluster.BaseNode;

import java.util.*;

/**
 * The nodes of the cloud, as last sent by the local node ({@code SyncType.NODES}).
 */
public final class NodeProvider implements INodeProvider {

    private volatile Map<String, INode> nodes = Map.of();

    public void syncIn(Collection<BaseNode> received) {
        Map<String, INode> map = new LinkedHashMap<>();
        for (BaseNode node : received) map.put(node.name(), node);
        nodes = Collections.unmodifiableMap(map);
    }

    @Override
    public Collection<INode> getAll() {
        return List.copyOf(nodes.values());
    }

    @Override
    public Optional<INode> get(String name) {
        return Optional.ofNullable(name == null ? null : nodes.get(name));
    }
}
