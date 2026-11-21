package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
public final class NodeHelloResponsePacket extends ResponsePacket {

    private boolean accepted;
    private String reason;
    private NodeIdentity identity;
    private Map<String, Object> members;

    public NodeHelloResponsePacket(boolean accepted, String reason, NodeIdentity identity, Map<String, Object> members) {
        this.accepted = accepted;
        this.reason = reason == null ? "" : reason;
        this.identity = identity;
        this.members = members;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(accepted, reason, identity == null ? Map.of() : identity.write(), members == null ? Map.of() : members);
    }

    @Override
    public void decodePayload(IPacketData data) {
        accepted = data.readBool();
        reason = data.readString();
        Map<String, Object> id = data.readMap();
        identity = id.isEmpty() ? null : NodeIdentity.read(id);
        members = data.readMap();
    }
}
