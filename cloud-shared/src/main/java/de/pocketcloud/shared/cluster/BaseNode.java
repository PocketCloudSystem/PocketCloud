package de.pocketcloud.shared.cluster;

import de.pocketcloud.api.cluster.INode;
import de.pocketcloud.common.serialization.MapperUtils;
import de.pocketcloud.common.serialization.Writable;
import de.pocketcloud.common.serialization.annotation.MapCreator;
import de.pocketcloud.common.serialization.annotation.MapKey;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Data view of a node, as synced to bridges ({@code SyncType.NODES}).
 */
@Getter
@Accessors(fluent = true)
public class BaseNode implements INode, Writable<Map<String, Object>> {

    private final String name;
    private final String address;
    private final int port;
    private final String state;
    private final boolean head;

    @MapCreator
    public BaseNode(
            @MapKey(name = "name") String name,
            @MapKey(name = "address") String address,
            @MapKey(name = "port") int port,
            @MapKey(name = "state") String state,
            @MapKey(name = "head") boolean head
    ) {
        this.name = name;
        this.address = address;
        this.port = port;
        this.state = state;
        this.head = head;
    }

    public static BaseNode of(INode node) {
        return new BaseNode(node.name(), node.address(), node.port(), node.state(), node.head());
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static BaseNode read(Map<String, Object> data) {
        return MapperUtils.fromMap(data, BaseNode.class);
    }

    @Override
    public String toString() {
        return "Node[" + name + ", " + state + (head ? ", head" : "") + "]";
    }
}
