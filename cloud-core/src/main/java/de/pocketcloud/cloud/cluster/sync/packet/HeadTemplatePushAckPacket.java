package de.pocketcloud.cloud.cluster.sync.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public final class HeadTemplatePushAckPacket extends ResponsePacket {

    private boolean accepted;
    private String message;

    public HeadTemplatePushAckPacket(boolean accepted, String message) {
        this.accepted = accepted;
        this.message = message == null ? "" : message;
    }

    @Override
    public void encodePayload(IPacketData data) {
        data.writeAll(accepted, message);
    }

    @Override
    public void decodePayload(IPacketData data) {
        accepted = data.readBool();
        message = data.readString();
    }
}
