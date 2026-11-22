package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.NoArgsConstructor;

/**
 * New head → node: which versions of the units the node has (applied, or served while it was head).
 * Answered with {@link NodeTemplateStatePacket}. The new head picks the newest version per unit from the answers
 * instead of trusting its own disk copy.
 */
@NoArgsConstructor
public final class HeadTemplateStateRequestPacket extends RequestPacket {

    @Override
    public void encodePayload(IPacketData data) {
    }

    @Override
    public void decodePayload(IPacketData data) {
    }
}
