package de.pocketcloud.network.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;
import lombok.Getter;
import lombok.Setter;

public abstract class ResponsePacket extends CloudPacket {

    @Getter
    @Setter
    private String requestId = "";

    @Override
    public final void encode(IPacketData packetData) {
        super.encode(packetData);
        packetData.write(requestId);
    }

    @Override
    public final void decode(IPacketData packetData) {
        super.decode(packetData);
        requestId = packetData.readString();
    }
}