package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerTransferPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket, AuthenticatedPacket {

    private String player;
    private String server;

    public PlayerTransferPacket(String player, String server) {
        this.player = player != null ? player : "";
        this.server = server != null ? server : "";
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(player, server);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.player = packetData.readString();
        this.server = packetData.readString();
    }

    public static PlayerTransferPacket create(String player, String server) {
        return new PlayerTransferPacket(player, server);
    }
}
