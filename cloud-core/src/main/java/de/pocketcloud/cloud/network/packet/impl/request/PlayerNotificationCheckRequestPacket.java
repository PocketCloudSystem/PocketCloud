package de.pocketcloud.cloud.network.packet.impl.request;

import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.cloud.cache.NotificationListCache;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.RequestPacket;
import de.pocketcloud.cloud.network.packet.impl.response.PlayerNotificationCheckResponsePacket;
import de.pocketcloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class PlayerNotificationCheckRequestPacket extends RequestPacket {

    private String player;

    public PlayerNotificationCheckRequestPacket(String player) {
        this.player = player != null ? player : "";
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        sendResponse(PlayerNotificationCheckResponsePacket.create(LocalCache.get(NotificationListCache.class).contains(player)), client);
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.player = packetData.readString();
    }

    public static PlayerNotificationCheckRequestPacket create(String player) {
        return new PlayerNotificationCheckRequestPacket(player);
    }
}