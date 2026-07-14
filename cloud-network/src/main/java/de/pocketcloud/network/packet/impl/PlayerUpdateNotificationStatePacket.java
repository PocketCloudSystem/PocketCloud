package de.pocketcloud.network.packet.impl;

import de.pocketcloud.api.network.packet.AuthenticatedPacket;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.network.packet.CloudPacket;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public final class PlayerUpdateNotificationStatePacket extends CloudPacket implements CloudboundPacket, AuthenticatedPacket {

    private String player;
    private boolean value;

    public PlayerUpdateNotificationStatePacket(String player, boolean value) {
        this.player = player != null ? player : "";
        this.value = value;
    }

    @Override
    public void encodePayload(IPacketData packetData) {}

    @Override
    public void decodePayload(IPacketData packetData) {
        this.player = packetData.readString();
        this.value = packetData.readBool();
    }

    public static PlayerUpdateNotificationStatePacket create(String player, boolean value) {
        return new PlayerUpdateNotificationStatePacket(player, value);
    }
}
