package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Answer to {@link NodeTemplateChunkRequestPacket}: up to 256 KiB of the file, Base64 encoded.
 */
@Getter
@NoArgsConstructor
public final class NodeTemplateChunkPacket extends ResponsePacket {

    private boolean ok;
    private String error;
    private long totalSize;
    private String data;

    public NodeTemplateChunkPacket(boolean ok, String error, long totalSize, String data) {
        this.ok = ok;
        this.error = error == null ? "" : error;
        this.totalSize = totalSize;
        this.data = data == null ? "" : data;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(ok, error, totalSize, this.data);
    }

    @Override
    public void decodePayload(IPacketData data) {
        ok = data.readBool();
        error = data.readString();
        totalSize = data.readLong();
        this.data = data.readString();
    }
}
