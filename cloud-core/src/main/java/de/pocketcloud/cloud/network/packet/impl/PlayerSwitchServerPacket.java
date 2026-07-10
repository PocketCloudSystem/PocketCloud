package de.pocketcloud.cloud.network.packet.impl;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.player.PlayerSwitchServerEvent;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.notification.Notifier;
import de.pocketcloud.network.packet.AuthenticatedPacket;
import de.pocketcloud.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.network.packet.type.NotificationType;
import de.pocketcloud.network.packet.data.PacketData;
import de.pocketcloud.cloud.player.CloudPlayerManager;
import de.pocketcloud.cloud.server.CloudServerManager;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@NoArgsConstructor
@Getter
public final class PlayerSwitchServerPacket extends CloudPacket implements CloudboundPacket, AuthenticatedPacket {

    private String player;
    private String newServer;

    public PlayerSwitchServerPacket(String player, String newServer) {
        this.player = player != null ? player : "";
        this.newServer = newServer != null ? newServer : "";
    }

    @Override
    public void handle(@NotNull ServerClient client) {
        var cloudPlayer = CloudPlayerManager.instance().get(player).orElse(null);
        if (cloudPlayer != null) {
            var server = CloudServerManager.instance().get(newServer).orElse(null);
            if (server != null) {
                if (Notifier.canLog(NotificationType.PLAYER_SWITCHED_SERVER)) {
                    if (cloudPlayer.currentServerName() == null) {
                        CloudLogger.get().info("Player §b{} §rperformed an initial connect on §b{}§r.", cloudPlayer.name(), server.name());
                    } else {
                        CloudLogger.get().info("Player §b{} §rperformed a server switch from §b{} §rto §b{}§r.", cloudPlayer.name(), cloudPlayer.currentServerName(), server.name());
                    }
                }
                Notifier.notify(NotificationType.PLAYER_SWITCHED_SERVER, Map.of(
                    "player", player,
                    "old_server", cloudPlayer.currentServerName() != null ? cloudPlayer.currentServerName() : "None",
                    "new_server", newServer
                ), Map.of());
                new PlayerSwitchServerEvent(cloudPlayer, cloudPlayer.currentServer().orElse(null), server).call();
                cloudPlayer.setCurrentServer(server);
            }
        }
    }

    @Override
    public void encodePayload(PacketData packetData) {}

    @Override
    public void decodePayload(PacketData packetData) {
        this.player = packetData.readString();
        this.newServer = packetData.readString();
    }

    public static PlayerSwitchServerPacket create(String player, String newServer) {
        return new PlayerSwitchServerPacket(player, newServer);
    }
}
