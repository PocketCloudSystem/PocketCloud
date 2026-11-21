package de.pocketcloud.cloud.cluster;

import de.pocketcloud.cloud.cluster.net.packet.NodeIdentity;
import io.netty.channel.Channel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Runtime view of another node.
 */
@Getter
@Setter
@Accessors(fluent = true)
public final class Peer {

    public static final long SUSPECT_AFTER_MS = 5_000;
    public static final long DEAD_AFTER_MS = 10_000;

    private final String name;
    private volatile Channel channel;
    private volatile boolean outbound;
    private volatile NodeIdentity identity;
    private volatile long lastSeen;
    private volatile long term;
    private volatile String head = "";
    private volatile String state = NodeState.JOINING.name();
    private volatile Map<String, Object> stats = Map.of();
    /** Whether the servers of this node were already dropped after it went dead. */
    private volatile boolean serversDropped;

    public Peer(String name) {
        this.name = name;
    }

    public boolean connected() {
        Channel c = channel;
        return c != null && c.isActive();
    }

    public boolean alive(long now) {
        return lastSeen > 0 && now - lastSeen < DEAD_AFTER_MS;
    }

    public String health(long now) {
        if (lastSeen == 0 || now - lastSeen >= DEAD_AFTER_MS) return "DEAD";
        if (now - lastSeen >= SUSPECT_AFTER_MS) return "SUSPECT";
        return "ALIVE";
    }

    public long startedAt() {
        NodeIdentity id = identity;
        return id == null ? Long.MAX_VALUE : id.startedAt();
    }
}
