package de.pocketcloud.cloud.network.packet.impl.response;

import de.pocketcloud.cloud.network.packet.ResponsePacket;
import de.pocketcloud.cloud.network.packet.type.ActionFailureReason;
import de.pocketcloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerSaveResponsePacket extends ResponsePacket {

    private ActionFailureReason errorReason;

    public ServerSaveResponsePacket(ActionFailureReason errorReason) {
        this.errorReason = errorReason;
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(errorReason);
    }

    public static ServerSaveResponsePacket create(ActionFailureReason errorReason) {
        return new ServerSaveResponsePacket(errorReason);
    }
}
