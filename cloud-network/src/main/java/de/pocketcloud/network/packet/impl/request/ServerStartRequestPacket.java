package de.pocketcloud.network.packet.impl.request;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerStartRequestPacket extends RequestPacket implements AuthenticatedPacket, CloudboundPacket {

    private String templateName;
    private int count;

    public ServerStartRequestPacket(String templateName, int count) {
        this.templateName = templateName != null ? templateName : "";
        this.count = count;
    }

    @Override
    public void encodePayload(IPacketData packetData) {}

    @Override
    public void decodePayload(IPacketData packetData) {
        this.templateName = packetData.readString();
        this.count = packetData.readInt();
    }

    public static ServerStartRequestPacket create(String templateName, int count) {
        return new ServerStartRequestPacket(templateName, count);
    }
}
