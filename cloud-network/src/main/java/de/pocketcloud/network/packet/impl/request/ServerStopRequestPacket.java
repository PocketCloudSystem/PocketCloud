package de.pocketcloud.network.packet.impl.request;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerStopRequestPacket extends RequestPacket implements AuthenticatedPacket, CloudboundPacket {

    private String server;
    private boolean forcefully;

    public ServerStopRequestPacket(String server, boolean forcefully) {
        this.server = server != null ? server : "";
        this.forcefully = forcefully;
    }

    @Override
    public void encodePayload(IPacketData packetData) {}

    @Override
    public void decodePayload(IPacketData packetData) {
        this.server = packetData.readString();
        this.forcefully = packetData.readBool();
    }

    public static ServerStopRequestPacket create(String server, boolean forcefully) {
        return new ServerStopRequestPacket(server, forcefully);
    }
}
