package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@Getter
public final class PlayerConnectPacket extends CloudPacket implements CloudboundPacket, AuthenticatedPacket {

    private String playerName;
    private String address;
    private String xboxUserId;
    private UUID uniqueId;
    private int protocolVersion;
    private String gameVersion;

    public PlayerConnectPacket(String playerName, String address, String xboxUserId, UUID uniqueId, int protocolVersion, String gameVersion) {
        this.playerName = playerName;
        this.address = address;
        this.xboxUserId = xboxUserId;
        this.uniqueId = uniqueId;
        this.protocolVersion = protocolVersion;
        this.gameVersion = gameVersion;
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(playerName, address, xboxUserId, uniqueId.toString(), protocolVersion, gameVersion);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        playerName = packetData.readString();
        address = packetData.readString();
        xboxUserId = packetData.readString();
        uniqueId = UUID.fromString(packetData.readString());
        protocolVersion = packetData.readInt();
        gameVersion = packetData.readString();
    }

    public static PlayerConnectPacket create(String playerName, String address, String xboxUserId, UUID uniqueId, int protocolVersion, String gameVersion) {
        return new PlayerConnectPacket(playerName, address, xboxUserId, uniqueId, protocolVersion, gameVersion);
    }
}
