package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Answer to {@link NodeTemplateHistoryRequestPacket}: gzip+base64 JSON list of history records (oldest first,
 * only as many of the newest as fit into one packet).
 */
@Getter
@NoArgsConstructor
public final class NodeTemplateHistoryPacket extends ResponsePacket {

    private String records;

    public NodeTemplateHistoryPacket(String records) {
        this.records = records == null ? "" : records;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.write(records);
    }

    @Override
    public void decodePayload(IPacketData data) {
        records = data.readString();
    }
}
