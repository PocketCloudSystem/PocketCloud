package de.pocketcloud.cloud.network.packet.impl.request;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.RequestPacket;
import de.pocketcloud.network.packet.type.ActionFailureReason;
import de.pocketcloud.cloud.network.packet.impl.response.ServerSaveResponsePacket;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.server.CloudServerManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class ServerSaveRequestPacket extends RequestPacket implements AuthenticatedPacket {

    private String server;

    public ServerSaveRequestPacket(String server) {
        this.server = server != null ? server : "";
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        var cloudServer = CloudServerManager.instance().get(server);
        if (cloudServer.isEmpty()) {
            sendResponse(ServerSaveResponsePacket.create(ActionFailureReason.SERVER_NOT_FOUND), client);
            return;
        }

        CloudServerManager.instance().save(cloudServer.get())
            .thenSuccess(_ -> sendResponse(ServerSaveResponsePacket.create(ActionFailureReason.NONE), client))
            .failure(_ -> sendResponse(ServerSaveResponsePacket.create(ActionFailureReason.REQUEST_TIMEOUT), client));
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.server = packetData.readString();
    }

    public static ServerSaveRequestPacket create(String server) {
        return new ServerSaveRequestPacket(server);
    }
}
