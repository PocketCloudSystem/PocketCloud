package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerDisconnectPacket extends CloudPacket implements CloudboundPacket, AuthenticatedPacket {

    private String player;

    public PlayerDisconnectPacket(String player) {
        this.player = player;
    }

    @Override
    public void encodePayload(IPacketData packetData) {}

    @Override
    public void decodePayload(IPacketData packetData) {
        this.player = packetData.readString();
    }

    public static PlayerDisconnectPacket create(String player) {
        return new PlayerDisconnectPacket(player);
    }
}
