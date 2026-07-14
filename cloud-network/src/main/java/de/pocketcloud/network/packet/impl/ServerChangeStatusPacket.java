package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerChangeStatusPacket extends CloudPacket implements CloudboundPacket, AuthenticatedPacket {

    private String serverUuid;
    private ServerStatus status;

    public ServerChangeStatusPacket(String serverUuid, ServerStatus status) {
        this.serverUuid = serverUuid != null ? serverUuid : "";
        this.status = status;
    }

    @Override
    public void encodePayload(IPacketData packetData) {}

    @Override
    public void decodePayload(IPacketData packetData) {
        this.serverUuid = packetData.readString();
        this.status = packetData.readEnum(ServerStatus.class);
    }

    public static ServerChangeStatusPacket create(String serverUuid, ServerStatus status) {
        return new ServerChangeStatusPacket(serverUuid, status);
    }
}
