package de.pocketcloud.network.packet.impl.response;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.api.server.VerificationStatus;
import de.pocketcloud.network.packet.ResponsePacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerHandshakeResponsePacket extends ResponsePacket implements ClientboundPacket {

    private VerificationStatus verificationStatus;

    public ServerHandshakeResponsePacket(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(verificationStatus);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static ServerHandshakeResponsePacket create(VerificationStatus verificationStatus) {
        return new ServerHandshakeResponsePacket(verificationStatus);
    }
}
