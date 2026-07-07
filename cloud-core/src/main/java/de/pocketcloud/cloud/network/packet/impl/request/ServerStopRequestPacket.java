package de.pocketcloud.cloud.network.packet.impl.request;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.RequestPacket;
import de.pocketcloud.cloud.network.packet.type.ActionFailureReason;
import de.pocketcloud.cloud.network.packet.impl.response.ServerStopResponsePacket;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.server.CloudServerManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class ServerStopRequestPacket extends RequestPacket implements AuthenticatedPacket {

    private String server;
    private boolean forcefully;

    public ServerStopRequestPacket(String server, boolean forcefully) {
        this.server = server != null ? server : "";
        this.forcefully = forcefully;
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        if (!CloudServerManager.instance().stop(server, forcefully).isEmpty()) {
            sendResponse(ServerStopResponsePacket.create(ActionFailureReason.NONE), client);
        } else {
            sendResponse(ServerStopResponsePacket.create(ActionFailureReason.SERVER_NOT_FOUND), client);
        }
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.server = packetData.readString();
        this.forcefully = packetData.readBool();
    }

    public static ServerStopRequestPacket create(String server, boolean forcefully) {
        return new ServerStopRequestPacket(server, forcefully);
    }
}
