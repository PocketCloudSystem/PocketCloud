package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A node fetches a part of a file with known content hash from another node (from the head when it
 * catches up, the head from a node that pushed a change). Answered with {@link NodeTemplateChunkPacket}.
 */
@Getter
@NoArgsConstructor
public final class NodeTemplateChunkRequestPacket extends RequestPacket {

    private String unit;
    private String path;
    private String sha;
    private long offset;

    public NodeTemplateChunkRequestPacket(String unit, String path, String sha, long offset) {
        this.unit = unit;
        this.path = path;
        this.sha = sha;
        this.offset = offset;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(unit, path, sha, offset);
    }

    @Override
    public void decodePayload(IPacketData data) {
        unit = data.readString();
        path = data.readString();
        sha = data.readString();
        offset = data.readLong();
    }
}
