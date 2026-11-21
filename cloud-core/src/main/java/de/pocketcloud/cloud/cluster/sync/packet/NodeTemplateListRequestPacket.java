package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.NoArgsConstructor;

/**
 * Node → head: which units exist and what their valid versions are. Answered with {@link HeadTemplateListPacket}.
 */
@NoArgsConstructor
public final class NodeTemplateListRequestPacket extends RequestPacket {

    @Override
    public void encodePayload(IPacketData data) {
    }

    @Override
    public void decodePayload(IPacketData data) {
    }
}
