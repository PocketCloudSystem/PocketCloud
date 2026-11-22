package de.pocketcloud.cloud.cluster.data.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.NoArgsConstructor;

/**
 * Node → head: sent after a head change or reconnect, so changes the node missed while it was away are caught up.
 * Answered with {@link HeadDataSnapshotPacket}.
 */
@NoArgsConstructor
public final class NodeDataSnapshotRequestPacket extends RequestPacket {

    @Override
    public void encodePayload(IPacketData data) {
    }

    @Override
    public void decodePayload(IPacketData data) {
    }
}
