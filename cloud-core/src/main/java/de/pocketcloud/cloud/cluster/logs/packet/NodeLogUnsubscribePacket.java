package de.pocketcloud.cloud.cluster.logs.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Node → owning node: stop streaming the log of a server to me. */
@Getter
@NoArgsConstructor
public final class NodeLogUnsubscribePacket extends CloudPacket {

    private String serverName;

    public NodeLogUnsubscribePacket(String serverName) {
        this.serverName = serverName;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(serverName);
    }

    @Override
    public void decodePayload(IPacketData data) {
        serverName = data.readString();
    }
}
