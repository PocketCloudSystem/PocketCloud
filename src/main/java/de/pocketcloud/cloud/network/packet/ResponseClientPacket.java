package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.packet.util.PacketData;
import lombok.Getter;

/**
 * The reversed response: sub-servers send this back to the cloud in response to a RequestClientPacket.
 * Logic is reversed compared to the regular ResponsePacket — here the cloud is the receiver.
 * @see RequestClientPacket
 */
@Getter
public abstract class ResponseClientPacket extends CloudPacket implements CloudboundPacket {

    private String requestId = "";

    @Override
    public final void encode(PacketData packetData) {
        super.encode(packetData);
        packetData.write(requestId);
    }

    @Override
    public final void decode(PacketData packetData) {
        super.decode(packetData);
        requestId = packetData.readString();
    }

    @Override
    public final void encodePayload(PacketData packetData) {}
}