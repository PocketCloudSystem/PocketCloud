package de.pocketcloud.network.packet.impl.response;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.ResponsePacket;
import de.pocketcloud.network.packet.type.ActionFailureReason;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerSaveResponsePacket extends ResponsePacket implements ClientboundPacket {

    private ActionFailureReason errorReason;

    public ServerSaveResponsePacket(ActionFailureReason errorReason) {
        this.errorReason = errorReason;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(errorReason);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static ServerSaveResponsePacket create(ActionFailureReason errorReason) {
        return new ServerSaveResponsePacket(errorReason);
    }
}
