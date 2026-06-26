package de.pocketcloud.cloud.network.packet.impl.response;

import de.pocketcloud.cloud.network.packet.ResponsePacket;
import de.pocketcloud.cloud.network.packet.type.ActionFailureReason;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerStopResponsePacket extends ResponsePacket {

    private ActionFailureReason errorReason;

    public ServerStopResponsePacket(ActionFailureReason errorReason) {
        this.errorReason = errorReason;
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(errorReason);
    }

    public static ServerStopResponsePacket create(ActionFailureReason errorReason) {
        return new ServerStopResponsePacket(errorReason);
    }
}
