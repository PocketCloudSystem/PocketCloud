package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor
@Getter
public final class PlayerConnectPacket extends CloudPacket implements CloudboundPacket {

    private CloudPlayer player;

    public PlayerConnectPacket(CloudPlayer player) {
        this.player = player;
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        var server = client.server();
        if (server != null) {
            if (CloudPlayerManager.instance().get(player.name()).isEmpty()) {
                if (server.template().templateType().isServer()) player.setCurrentServer(server);
                else player.setCurrentProxy(server);
                CloudPlayerManager.instance().add(player);
            }
        }
    }

    @Override
    public void encodePayload(PacketData packetData) {}

    @Override
    public void decodePayload(PacketData packetData) {
        this.player = packetData.readPlayer();
    }

    public static PlayerConnectPacket create(CloudPlayer player) {
        return new PlayerConnectPacket(player);
    }
}
