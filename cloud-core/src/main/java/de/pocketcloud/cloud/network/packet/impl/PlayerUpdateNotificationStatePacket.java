package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.provider.CloudProvider;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
    public void handle(@NotNull ServerClient client) {
        if (value) CloudProvider.current().enablePlayerNotifications(player);
        else CloudProvider.current().disablePlayerNotifications(player);
    }

    @Override
    public void encodePayload(PacketData packetData) {}

    @Override
    public void decodePayload(PacketData packetData) {
        this.player = packetData.readString();
        this.value = packetData.readBool();
    }

    public static PlayerUpdateNotificationStatePacket create(String player, boolean value) {
        return new PlayerUpdateNotificationStatePacket(player, value);
    }
}
