package de.pocketcloud.cloud.network.packet.impl.response;

import de.pocketcloud.cloud.network.packet.ResponsePacket;
import de.pocketcloud.network.packet.type.VerificationStatus;
import de.pocketcloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerHandshakeResponsePacket extends ResponsePacket {

    private VerificationStatus verificationStatus;

    public ServerHandshakeResponsePacket(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(verificationStatus);
    }

    public static ServerHandshakeResponsePacket create(VerificationStatus verificationStatus) {
        return new ServerHandshakeResponsePacket(verificationStatus);
    }
}
