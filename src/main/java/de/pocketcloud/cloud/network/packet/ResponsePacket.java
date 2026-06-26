package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * The normal response packet sent from the cloud back to sub-servers after they sent a request via RequestPacket.
 * @see RequestPacket
 */
@Getter
public abstract class ResponsePacket extends CloudPacket implements ClientboundPacket {

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
    public final void decodePayload(PacketData packetData) {}

    @Override
    public final void handle(@NotNull ServerClient client) {}

    public ResponsePacket setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }
}