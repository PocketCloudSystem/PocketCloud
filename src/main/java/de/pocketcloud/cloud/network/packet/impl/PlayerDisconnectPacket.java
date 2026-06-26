package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class PlayerDisconnectPacket extends CloudPacket implements CloudboundPacket {

    private String player;

    public PlayerDisconnectPacket(String player) {
        this.player = player;
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        var cloudPlayer = CloudPlayerManager.instance().get(player).orElse(null);
        if (cloudPlayer != null) {
            if (cloudPlayer.currentProxy().isEmpty()) {
                CloudPlayerManager.instance().remove(cloudPlayer);
            } else {
                var server = client.server();
                if (server != null && server.template().templateType().isProxy()) {
                    CloudPlayerManager.instance().remove(cloudPlayer);
                }
            }
        }
    }

    @Override
    public void encodePayload(PacketData packetData) {}

    @Override
    public void decodePayload(PacketData packetData) {
        this.player = packetData.readString();
    }

    public static PlayerDisconnectPacket create(String player) {
        return new PlayerDisconnectPacket(player);
    }
}
