package de.pocketcloud.network.packet.impl.request;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.RequestPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerWhitelistCheckRequestPacket extends RequestPacket implements AuthenticatedPacket, CloudboundPacket {

    private String player;

    public PlayerWhitelistCheckRequestPacket(String player) {
        this.player = player != null ? player : "";
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(player);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.player = packetData.readString();
    }

    public static PlayerWhitelistCheckRequestPacket create(String player) {
        return new PlayerWhitelistCheckRequestPacket(player);
    }
}