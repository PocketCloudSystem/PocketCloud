package de.pocketcloud.cloud.network.packet.impl.request;

import de.pocketcloud.cloud.cache.LocalCache;
import de.pocketcloud.cloud.cache.WhitelistCache;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.RequestPacket;
import de.pocketcloud.cloud.network.packet.impl.response.PlayerWhitelistCheckResponsePacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class PlayerWhitelistCheckRequestPacket extends RequestPacket {

    private String player;

    public PlayerWhitelistCheckRequestPacket(String player) {
        this.player = player != null ? player : "";
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        sendResponse(PlayerWhitelistCheckResponsePacket.create(LocalCache.get(WhitelistCache.class).contains(player)), client);
    }

    @Override
    public void decodePayload(PacketData packetData) {
        this.player = packetData.readString();
    }

    public static PlayerWhitelistCheckRequestPacket create(String player) {
        return new PlayerWhitelistCheckRequestPacket(player);
    }
}