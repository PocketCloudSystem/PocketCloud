package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.server.CloudServer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class ServerSyncPacket extends CloudPacket implements ClientboundPacket {

    private CloudServer server;
    private boolean removal;

    public ServerSyncPacket(CloudServer server, boolean removal) {
        this.server = server;
        this.removal = removal;
    }

    @Override
    public void handle(@NotNull ServerClient client) {}

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(server, removal);
    }

    @Override
    public void decodePayload(PacketData packetData) {}

    public static ServerSyncPacket create(CloudServer server, boolean removal) {
        return new ServerSyncPacket(server, removal);
    }
}
