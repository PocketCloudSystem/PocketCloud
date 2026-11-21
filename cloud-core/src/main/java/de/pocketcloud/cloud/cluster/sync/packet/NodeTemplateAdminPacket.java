package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node → head: {@code template history} / {@code template rollback} typed on a node that is not head.
 * Answered with {@link HeadTemplateAdminResultPacket}.
 */
@Getter
@NoArgsConstructor
public final class NodeTemplateAdminPacket extends RequestPacket {

    private String action;
    private String unit;
    private long version;

    public NodeTemplateAdminPacket(String action, String unit, long version) {
        this.action = action;
        this.unit = unit;
        this.version = version;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(action, unit, version);
    }

    @Override
    public void decodePayload(IPacketData data) {
        action = data.readString();
        unit = data.readString();
        version = data.readLong();
    }
}
