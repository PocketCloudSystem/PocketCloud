package de.pocketcloud.bridge.platform.pnx.listener;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.language.LanguageKey;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.cache.WhitelistCache;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.network.packet.impl.PlayerConnectPacket;
import de.pocketcloud.network.packet.impl.PlayerDisconnectPacket;
import de.pocketcloud.shared.network.packet.type.NotificationType;
import org.powernukkitx.Player;
import org.powernukkitx.event.EventHandler;
import org.powernukkitx.event.EventPriority;
import org.powernukkitx.event.Listener;
import org.powernukkitx.event.player.PlayerKickEvent;
import org.powernukkitx.event.player.PlayerLoginEvent;
import org.powernukkitx.event.player.PlayerPreLoginEvent;
import org.powernukkitx.event.player.PlayerQuitEvent;

import java.util.Map;

public final class PlayerListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void handle(PlayerPreLoginEvent event) {
        if (event.isCancelled()) {
            CloudBridge.instance().notifications().sendNotification(NotificationType.PLAYER_JOIN_FAILED, Map.of(
                    "player", event.getIdentityClaims().extraData.displayName,
                    "reason", event.getKickMessage(),
                    "server", CloudBridge.instance().environmentConfig().localServerName()
            ));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void handle(PlayerLoginEvent event) {
        Player player = event.getPlayer();
        if (CloudAPI.instance().templates().current().settings().maintenance() &&
                !LocalCache.get(WhitelistCache.class).contains(player.getName()) &&
                !player.hasPermission("pocketcloud.bypass.maintenance")) {
            event.setKickMessage(LanguageKey.INGAME_TEMPLATE_KICK_MAINTENANCE.translate());
            event.setCancelled();
            CloudBridge.instance().notifications().sendNotification(NotificationType.PLAYER_JOIN_FAILED, Map.of(
                    "player", player.getName(),
                    "server", CloudBridge.instance().environmentConfig().localServerName(),
                    "reason", "Template is in maintenance"
            ));
            return;
        }

        if (event.isCancelled()) {
            CloudBridge.instance().notifications().sendNotification(NotificationType.PLAYER_JOIN_FAILED, Map.of(
                    "player", player.getName(),
                    "server", CloudBridge.instance().environmentConfig().localServerName(),
                    "reason", event.getKickMessage()
            ));
        }

        PlayerConnectPacket.create(
                player.getName(),
                player.getAddress(),
                player.getXUID(),
                player.getUniqueId(),
                player.getSession().getPeer().getCodec().getProtocolVersion(),
                player.getSession().getPeer().getCodec().getMinecraftVersion()
        ).sendPacket();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void handle(PlayerQuitEvent event) {
        PlayerDisconnectPacket.create(event.getPlayer().getName()).sendPacket();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void handle(PlayerKickEvent event) {
        Player player = event.getPlayer();
        String finalReason = event.getReason();
        if (event.getPlayer().spawned) {
            CloudBridge.instance().notifications().sendNotification(NotificationType.PLAYER_KICKED, Map.of(
                    "player", player.getName(),
                    "server", CloudBridge.instance().environmentConfig().localServerName(),
                    "reason", finalReason
            ));
        } else {
            CloudBridge.instance().notifications().sendNotification(NotificationType.PLAYER_JOIN_FAILED, Map.of(
                    "player", player.getName(),
                    "server", CloudBridge.instance().environmentConfig().localServerName(),
                    "reason", finalReason
            ));
        }
    }
}