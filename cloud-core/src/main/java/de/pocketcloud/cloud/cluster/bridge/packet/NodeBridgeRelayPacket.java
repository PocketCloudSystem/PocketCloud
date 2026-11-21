package de.pocketcloud.cloud.cluster.bridge.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Any node → owning node: deliver a bridge packet (e.g. {@code PlayerTextPacket}, {@code PlayerTransferPacket},
 * {@code PlayerKickPacket}) to one of your servers. The bridge packet is carried in its encoded form (Base64).
 */
@Getter
@NoArgsConstructor
public final class NodeBridgeRelayPacket extends CloudPacket {

    private String serverName;
    private String packetName;
    private String payload;

    public NodeBridgeRelayPacket(String serverName, String packetName, String payload) {
        this.serverName = serverName;
        this.packetName = packetName;
        this.payload = payload;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(serverName, packetName, payload);
    }

    @Override
    public void decodePayload(IPacketData data) {
        serverName = data.readString();
        packetName = data.readString();
        payload = data.readString();
    }
}
