package de.pocketcloud.cloud.network.packet.impl.request;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.server.ServerPostVerificationEvent;
import de.pocketcloud.cloud.event.impl.server.ServerVerifyEvent;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.client.ServerClientCache;
import de.pocketcloud.cloud.network.packet.RequestPacket;
import de.pocketcloud.network.packet.type.VerificationStatus;
import de.pocketcloud.cloud.network.packet.impl.response.ServerHandshakeResponsePacket;
import de.pocketcloud.common.util.NumberUtils;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.server.CloudServerManager;
import de.pocketcloud.cloud.server.util.ServerStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class ServerHandshakeRequestPacket extends RequestPacket {

    private String serverName;
    private long processId;
    private int maxPlayers;

    public ServerHandshakeRequestPacket(String serverName, long processId, int maxPlayers) {
        this.serverName = serverName != null ? serverName : "";
        this.processId = processId;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        var server = CloudServerManager.instance().get(serverName).orElse(null);
        if (server == null) return;
        if (ServerClientCache.instance().getServer(client) == null) {
            var ev = new ServerVerifyEvent(server);
            ev.call();
            if (ev.isCancelled()) {
                server.logger().warn("Denied server handshake request by event §8(§b{}§8)", client.address());
                return;
            }

            float elapsed = NumberUtils.formatNumber((float) (System.currentTimeMillis() - server.startTime()) / 1000, 3);

            ServerClientCache.instance().add(server, client);
            CloudLogger.get().success("The server §b{} §rhas §aconnected §rto the cloud. §8(§rTook §b{}§rs§8)", server.name(), elapsed);
            server.serverData().maxPlayers(maxPlayers);
            server.serverData().processId(processId);
            server.verifiedTime(System.currentTimeMillis());
            server.verificationStatus(VerificationStatus.VERIFIED);
            server.addToProxies();
            server.sync();
            new ServerPostVerificationEvent(server).call();
            sendResponse(new ServerHandshakeResponsePacket(VerificationStatus.VERIFIED), client);
            server.setStatus(ServerStatus.ONLINE);
        } else {
            server.logger().warn("Denied server handshake request, duplicate server... §8(§b{}§r§8)", client.address());
            CloudLogger.get().warn("Denied server handshake request from §b{} §8(§b{}§8)§r, duplicate server...", serverName, client.address());
            sendResponse(new ServerHandshakeResponsePacket(VerificationStatus.DENIED), client);
        }
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.serverName = packetData.readString();
        this.processId = packetData.readLong();
        this.maxPlayers = packetData.readInt();
    }

    public static ServerHandshakeRequestPacket create(String serverName, int processId, int maxPlayers) {
        return new ServerHandshakeRequestPacket(serverName, processId, maxPlayers);
    }
}
