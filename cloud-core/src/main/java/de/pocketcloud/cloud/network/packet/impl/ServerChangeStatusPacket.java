package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.server.CloudServerManager;
import de.pocketcloud.cloud.server.util.ServerStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
    public void handle(@NotNull ServerClient client) {
        CloudServerManager.instance().get(serverUuid).ifPresent(server -> server.setStatus(status));
    }

    @Override
    public void encodePayload(PacketData packetData) {}

    @Override
    public void decodePayload(PacketData packetData) {
        this.serverUuid = packetData.readString();
        this.status = packetData.readEnum(ServerStatus.class);
    }

    public static ServerChangeStatusPacket create(String serverUuid, ServerStatus status) {
        return new ServerChangeStatusPacket(serverUuid, status);
    }
}
