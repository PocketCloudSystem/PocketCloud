package de.pocketcloud.cloud.cluster.data.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node → head: a write to the cluster-wide data (whitelist, notifications, templates, groups) that was made on a
 * node that is not head. The head executes it and answers with {@link HeadDataWriteResultPacket}.
 * {@code payload} is JSON (or empty), its meaning depends on the operation.
 */
@Getter
@NoArgsConstructor
public final class NodeDataWritePacket extends RequestPacket {

    private String operation;
    private String target;
    private String payload;

    public NodeDataWritePacket(String operation, String target, String payload) {
        this.operation = operation;
        this.target = target;
        this.payload = payload == null ? "" : payload;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(operation, target, payload);
    }

    @Override
    public void decodePayload(IPacketData data) {
        operation = data.readString();
        target = data.readString();
        payload = data.readString();
    }
}
