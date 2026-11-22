package de.pocketcloud.network.packet.impl.request;

import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@Getter
public final class ServerHandshakeRequestPacket extends RequestPacket implements CloudboundPacket {

    private String serverName;
    private long processId;
    private int maxPlayers;
    /**
     * UUID of the server the bridge belongs to (empty for older bridges). The cloud only accepts a bridge whose uuid
     * matches the server it expects, so a stale process with the same name cannot take over a new server's slot.
     */
    private String serverUuid = "";

    public ServerHandshakeRequestPacket(String serverName, long processId, int maxPlayers) {
        this(serverName, null, processId, maxPlayers);
    }

    public ServerHandshakeRequestPacket(String serverName, UUID serverUuid, long processId, int maxPlayers) {
        this.serverName = serverName != null ? serverName : "";
        this.serverUuid = serverUuid != null ? serverUuid.toString() : "";
        this.processId = processId;
        this.maxPlayers = maxPlayers;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(serverName, processId, maxPlayers, serverUuid);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.serverName = packetData.readString();
        this.processId = packetData.readLong();
        this.maxPlayers = packetData.readInt();
        this.serverUuid = packetData.remaining() > 0 ? packetData.readString() : "";
    }

    public static ServerHandshakeRequestPacket create(String serverName, long processId, int maxPlayers) {
        return new ServerHandshakeRequestPacket(serverName, processId, maxPlayers);
    }

    public static ServerHandshakeRequestPacket create(String serverName, UUID serverUuid, long processId, int maxPlayers) {
        return new ServerHandshakeRequestPacket(serverName, serverUuid, processId, maxPlayers);
    }
}
