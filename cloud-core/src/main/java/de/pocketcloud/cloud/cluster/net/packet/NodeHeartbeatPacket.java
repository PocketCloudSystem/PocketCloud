package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Sent every second to every connected node. Carries the sender's view of the cluster
 * (term, head) and the member list, so membership changes spread to all nodes.
 */
@Getter
@NoArgsConstructor
public final class NodeHeartbeatPacket extends CloudPacket {

    private long term;
    private String head;
    private String state;
    private Map<String, Object> stats;
    private Map<String, Object> members;

    public NodeHeartbeatPacket(long term, String head, String state, Map<String, Object> stats, Map<String, Object> members) {
        this.term = term;
        this.head = head == null ? "" : head;
        this.state = state;
        this.stats = stats;
        this.members = members;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(term, head, state, stats, members);
    }

    @Override
    public void decodePayload(IPacketData data) {
        term = data.readLong();
        head = data.readString();
        state = data.readString();
        stats = data.readMap();
        members = data.readMap();
    }
}
