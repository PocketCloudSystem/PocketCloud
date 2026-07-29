package de.pocketcloud.network.packet.impl.request;

import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class ServerHandshakeRequestPacket extends RequestPacket implements CloudboundPacket {

    private String serverName;
    private long processId;
    private int maxPlayers;

    public ServerHandshakeRequestPacket(String serverName, long processId, int maxPlayers) {
        this.serverName = serverName != null ? serverName : "";
        this.processId = processId;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(serverName, processId, maxPlayers);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.serverName = packetData.readString();
        this.processId = packetData.readLong();
        this.maxPlayers = packetData.readInt();
    }

    public static ServerHandshakeRequestPacket create(String serverName, long processId, int maxPlayers) {
        return new ServerHandshakeRequestPacket(serverName, processId, maxPlayers);
    }
}
