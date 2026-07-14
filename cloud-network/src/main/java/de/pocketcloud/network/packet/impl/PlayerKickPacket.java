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
public final class PlayerKickPacket extends CloudPacket implements ClientboundPacket, CloudboundPacket, AuthenticatedPacket {

    private String player;
    private String reason;
    private String disconnectScreenMessage;

    public PlayerKickPacket(String player, String reason, String disconnectScreenMessage) {
        this.player = player != null ? player : "";
        this.reason = reason != null ? reason : "";
        this.disconnectScreenMessage = disconnectScreenMessage != null ? disconnectScreenMessage : "";
    }

    @Override
    public void encodePayload(IPacketData packetData) {
        packetData.writeAll(player, reason, disconnectScreenMessage);
    }

    @Override
    public void decodePayload(IPacketData packetData) {
        this.player = packetData.readString();
        this.reason = packetData.readString();
        this.disconnectScreenMessage = packetData.readString();
    }

    public static PlayerKickPacket create(String player, String reason, String disconnectScreenMessage) {
        return new PlayerKickPacket(player, reason, disconnectScreenMessage);
    }
}
