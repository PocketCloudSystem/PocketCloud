package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.model.server.ICloudServer;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerSyncPacket extends CloudPacket implements ClientboundPacket {

    private ICloudServer server;
    private boolean removal;

    public ServerSyncPacket(ICloudServer server, boolean removal) {
        this.server = server;
        this.removal = removal;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(server, removal);
    }

    @Override
    public void decodePayload(IPacketData packetData) {}

    public static ServerSyncPacket create(ICloudServer server, boolean removal) {
        return new ServerSyncPacket(server, removal);
    }
}
