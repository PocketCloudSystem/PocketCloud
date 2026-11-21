package de.pocketcloud.cloud.cluster.net;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.cloud.cluster.net.packet.*;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Packets of the node-to-node protocol. Separate from the bridge registry; all names start
 * with {@code Node} or {@code Head}.
 */
public final class ClusterPacketRegistry {

    private final Map<String, Constructor<? extends Packet>> packets = new ConcurrentHashMap<>();

    public ClusterPacketRegistry() {
        register(NodeHelloPacket.class);
        register(NodeHelloResponsePacket.class);
        register(NodeHeartbeatPacket.class);
        register(NodeLeavePacket.class);
        register(HeadClaimPacket.class);
        register(HeadVotePacket.class);
        register(HeadAnnouncePacket.class);
    }

    private void register(Class<? extends Packet> type) {
        try {
            packets.put(type.getSimpleName(), type.getDeclaredConstructor());
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(type.getName() + " needs a no-args constructor", e);
        }
    }

    public Packet create(String name) {
        Constructor<? extends Packet> constructor = packets.get(name);
        if (constructor == null) return null;
        try {
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
