package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * First packet on every node connection, sent by the side that opened it.
 */
@Getter
@NoArgsConstructor
public final class NodeHelloPacket extends RequestPacket {

    private NodeIdentity identity;
    private int protocolVersion;

    public NodeHelloPacket(NodeIdentity identity, int protocolVersion) {
        this.identity = identity;
        this.protocolVersion = protocolVersion;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.write(identity.write());
        data.write(protocolVersion);
    }

    @Override
    public void decodePayload(IPacketData data) {
        identity = NodeIdentity.read(data.readMap());
        protocolVersion = data.readInt();
    }
}
