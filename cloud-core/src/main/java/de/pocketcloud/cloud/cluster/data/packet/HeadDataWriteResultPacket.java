package de.pocketcloud.cloud.cluster.data.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Result of a {@link NodeDataWritePacket} on the head. */
@Getter
@NoArgsConstructor
public final class HeadDataWriteResultPacket extends ResponsePacket {

    private boolean success;
    private String message;

    public HeadDataWriteResultPacket(boolean success, String message) {
        this.success = success;
        this.message = message == null ? "" : message;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(success, message);
    }

    @Override
    public void decodePayload(IPacketData data) {
        success = data.readBool();
        message = data.readString();
    }
}
