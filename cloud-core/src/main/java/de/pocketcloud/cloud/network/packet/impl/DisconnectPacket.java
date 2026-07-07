package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.ClientboundPacket;
import de.pocketcloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.type.ServerDisconnectReason;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.server.CloudServersHandler;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class DisconnectPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket, AuthenticatedPacket {

    private ServerDisconnectReason reason;

    public DisconnectPacket(ServerDisconnectReason reason) {
        this.reason = reason;
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        CloudServersHandler.handleDisconnect(client.server(), reason);
    }

    @Override
    public void encodePayload(PacketData packetData) {
        packetData.writeAll(reason);
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.reason = packetData.readEnum(ServerDisconnectReason.class);
    }

    public static DisconnectPacket create(ServerDisconnectReason reason) {
        return new DisconnectPacket(reason);
    }
}
