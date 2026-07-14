package de.pocketcloud.network.packet.impl.request;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerSaveRequestPacket extends RequestPacket implements AuthenticatedPacket, CloudboundPacket {

    private String server;

    public ServerSaveRequestPacket(String server) {
        this.server = server != null ? server : "";
    }

    @Override
    public void encodePayload(IPacketData packetData) {}

    @Override
    public void decodePayload(IPacketData packetData) {
        this.server = packetData.readString();
    }

    public static ServerSaveRequestPacket create(String server) {
        return new ServerSaveRequestPacket(server);
    }
}
