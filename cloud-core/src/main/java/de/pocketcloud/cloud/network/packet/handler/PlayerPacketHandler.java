package de.pocketcloud.cloud.network.packet.handler;

import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.player.PlayerSwitchServerEvent;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.notification.Notifier;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.cloud.cache.NotificationListCache;
import de.pocketcloud.cloud.cache.WhitelistCache;
import de.pocketcloud.network.packet.impl.*;
import de.pocketcloud.network.packet.impl.request.PlayerNotificationCheckRequestPacket;
import de.pocketcloud.network.packet.impl.request.PlayerWhitelistCheckRequestPacket;
import de.pocketcloud.network.packet.impl.response.PlayerNotificationCheckResponsePacket;
import de.pocketcloud.network.packet.impl.response.PlayerWhitelistCheckResponsePacket;
import de.pocketcloud.network.packet.type.NotificationType;

import java.util.Map;

public final class PlayerPacketHandler implements PacketListener {

    @PacketHandler({PlayerNotificationCheckRequestPacket.class})
    public void handle(PlayerNotificationCheckRequestPacket packet, ServerClient sender) {
        packet.sendResponse(PlayerNotificationCheckResponsePacket.create(LocalCache.get(NotificationListCache.class).contains(packet.getPlayer())), sender);
    }

    @PacketHandler({PlayerWhitelistCheckRequestPacket.class})
    public void handle(PlayerWhitelistCheckRequestPacket packet, ServerClient sender) {
        packet.sendResponse(PlayerWhitelistCheckResponsePacket.create(LocalCache.get(WhitelistCache.class).contains(packet.getPlayer())), sender);
    }

    @PacketHandler({PlayerConnectPacket.class})
    public void handle(PlayerConnectPacket packet, ServerClient sender) {
        var server = sender.server();
        if (server != null) {
            CloudPlayer player = new CloudPlayer(packet.getPlayerName(), packet.getAddress(), packet.getXboxUserId(), packet.getUniqueId(), packet.getProtocolVersion(), packet.getGameVersion());
            if (PocketCloud.instance().players().get(player.name()).isEmpty()) {
                if (server.template().templateType().isServer()) player.setCurrentServer(server);
                else player.setCurrentProxy(server);
                PocketCloud.instance().players().add(player);
            }
        }
    }

    @PacketHandler({PlayerDisconnectPacket.class})
    public void handle(PlayerDisconnectPacket packet, ServerClient sender) {
        var cloudPlayer = PocketCloud.instance().players().get(packet.getPlayer()).orElse(null);
        if (cloudPlayer != null) {
            if (cloudPlayer.currentProxy().isEmpty()) {
                PocketCloud.instance().players().remove(cloudPlayer);
            } else {
                var server = sender.server();
                if (server != null && server.template().templateType().isProxy()) {
                    PocketCloud.instance().players().remove(cloudPlayer);
                }
            }
        }
    }

    @PacketHandler({PlayerKickPacket.class})
    public void handle(PlayerKickPacket packet, ServerClient sender) {
        PocketCloud.instance().players().get(packet.getPlayer()).ifPresent(p -> p.kick(packet.getReason(), packet.getDisconnectScreenMessage()));
    }

    @PacketHandler({PlayerSwitchServerPacket.class})
    public void handle(PlayerSwitchServerPacket packet, ServerClient sender) {
        var cloudPlayer = PocketCloud.instance().players().get(packet.getPlayer()).orElse(null);
        if (cloudPlayer != null) {
            var server = PocketCloud.instance().servers().get(packet.getNewServer()).orElse(null);
            if (server != null) {
                if (Notifier.canLog(NotificationType.PLAYER_SWITCHED_SERVER)) {
                    if (cloudPlayer.currentServerName() == null) {
                        CloudLogger.get().info("Player §b{} §rperformed an initial connect on §b{}§r.", cloudPlayer.name(), server.name());
                    } else {
                        CloudLogger.get().info("Player §b{} §rperformed a server switch from §b{} §rto §b{}§r.", cloudPlayer.name(), cloudPlayer.currentServerName(), server.name());
                    }
                }
                Notifier.notify(NotificationType.PLAYER_SWITCHED_SERVER, Map.of(
                    "player", packet.getPlayer(),
                    "old_server", cloudPlayer.currentServerName() != null ? cloudPlayer.currentServerName() : "None",
                    "new_server", packet.getNewServer()
                ), Map.of());
                new PlayerSwitchServerEvent(cloudPlayer, cloudPlayer.currentServer().orElse(null), server).call();
                cloudPlayer.setCurrentServer(server);
            }
        }
    }

    @PacketHandler({PlayerTextPacket.class})
    public void handle(PlayerTextPacket packet, ServerClient sender) {
        PocketCloud.instance().players().get(packet.getPlayer()).ifPresent(p -> p.send(packet.getText(), packet.getType()));
    }

    @PacketHandler({PlayerTransferPacket.class})
    public void handle(PlayerTransferPacket packet, ServerClient sender) {
        PocketCloud.instance().players().get(packet.getPlayer()).flatMap(CloudPlayer::currentProxy).ifPresent(proxy -> proxy.sendPacket(packet));
    }

    @PacketHandler({PlayerUpdateNotificationStatePacket.class})
    public void handle(PlayerUpdateNotificationStatePacket packet, ServerClient sender) {
        if (packet.isValue()) CloudProvider.current().enablePlayerNotifications(packet.getPlayer());
        else CloudProvider.current().disablePlayerNotifications(packet.getPlayer());
    }
}
