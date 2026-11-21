package de.pocketcloud.cloud.cluster.net.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.NoArgsConstructor;

/**
 * Clean shutdown: receivers mark the sender as dead right away instead of waiting for the timeout.
 */
@NoArgsConstructor
public final class NodeLeavePacket extends CloudPacket {

    @Override
    public void encodePayload(IPacketData data) {
        data.write(0);
    }

    @Override
    public void decodePayload(IPacketData data) {
        data.read();
    }
}
