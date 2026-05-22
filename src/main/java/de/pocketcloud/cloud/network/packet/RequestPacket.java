package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.packet.util.PacketData;
import io.netty.channel.Channel;
import lombok.Getter;

/**
 * The normal request packet sent from sub-servers to the cloud, which will answer through a regular ResponsePacket.
 * @see ResponsePacket
 */
@Getter
public abstract class RequestPacket extends CloudPacket implements CloudboundPacket {

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

    public void sendResponse(ResponsePacket packet, Channel channel) {
        packet.setRequestId(requestId);
        channel.writeAndFlush(packet);
    }
}