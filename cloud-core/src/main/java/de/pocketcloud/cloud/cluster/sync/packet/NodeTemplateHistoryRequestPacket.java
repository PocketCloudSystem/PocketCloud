package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Node → head (or new head → node): the history records of a unit newer than {@code afterVersion}.
 * Answered with {@link NodeTemplateHistoryPacket} by every node from its own copy of the history.
 */
@Getter
@NoArgsConstructor
public final class NodeTemplateHistoryRequestPacket extends RequestPacket {

    private String unit;
    private long afterVersion;

    public NodeTemplateHistoryRequestPacket(String unit, long afterVersion) {
        this.unit = unit;
        this.afterVersion = afterVersion;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(unit, afterVersion);
    }

    @Override
    public void decodePayload(IPacketData data) {
        unit = data.readString();
        afterVersion = data.readLong();
    }
}
