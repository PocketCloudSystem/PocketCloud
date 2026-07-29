package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerSwitchServerPacket extends CloudPacket implements CloudboundPacket, AuthenticatedPacket {

    private String player;
    private String newServer;

    public PlayerSwitchServerPacket(String player, String newServer) {
        this.player = player != null ? player : "";
        this.newServer = newServer != null ? newServer : "";
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(player, newServer);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.player = packetData.readString();
        this.newServer = packetData.readString();
    }

    public static PlayerSwitchServerPacket create(String player, String newServer) {
        return new PlayerSwitchServerPacket(player, newServer);
    }
}
