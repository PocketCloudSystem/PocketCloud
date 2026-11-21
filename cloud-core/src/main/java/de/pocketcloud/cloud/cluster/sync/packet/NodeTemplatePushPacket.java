package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node → head: local changes of a unit, as a delta against the version the node last took over.
 * {@code changes}: gzip+base64 JSON rows {@code [path, size, sha, baseSha]} (baseSha empty for new files),
 * {@code removed}: rows {@code [path, baseSha]}. {@code unitDeleted}: the whole folder was removed.
 * The head acknowledges at once with {@link HeadTemplatePushAckPacket}, fetches the files it does not
 * have from the node and announces the result with {@link HeadTemplateVersionPacket}.
 */
@Getter
@NoArgsConstructor
public final class NodeTemplatePushPacket extends RequestPacket {

    private String unit;
    private long baseVersion;
    private boolean unitDeleted;
    private String changes;
    private String removed;

    public NodeTemplatePushPacket(String unit, long baseVersion, boolean unitDeleted, String changes, String removed) {
        this.unit = unit;
        this.baseVersion = baseVersion;
        this.unitDeleted = unitDeleted;
        this.changes = changes;
        this.removed = removed;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(unit, baseVersion, unitDeleted, changes, removed);
    }

    @Override
    public void decodePayload(IPacketData data) {
        unit = data.readString();
        baseVersion = data.readLong();
        unitDeleted = data.readBool();
        changes = data.readString();
        removed = data.readString();
    }
}
