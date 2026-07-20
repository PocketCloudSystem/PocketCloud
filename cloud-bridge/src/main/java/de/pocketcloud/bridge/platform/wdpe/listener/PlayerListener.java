package de.pocketcloud.bridge.platform.wdpe.listener;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.language.LanguageKey;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.cache.WhitelistCache;
import de.pocketcloud.common.cache.LocalCache;
import de.pocketcloud.network.packet.impl.PlayerConnectPacket;
import de.pocketcloud.network.packet.impl.PlayerDisconnectPacket;
import de.pocketcloud.network.packet.impl.PlayerSwitchServerPacket;
import de.pocketcloud.shared.network.packet.type.NotificationType;
import dev.waterdog.waterdogpe.event.defaults.InitialServerDeterminedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerDisconnectedEvent;
import dev.waterdog.waterdogpe.event.defaults.PlayerLoginEvent;
import dev.waterdog.waterdogpe.event.defaults.ServerTransferEvent;
import dev.waterdog.waterdogpe.player.ProxiedPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PlayerListener {

    private static final List<String> initialConnects = new ArrayList<>();

    public static void handle(PlayerLoginEvent event) {
        ProxiedPlayer player = event.getPlayer();

        if (CloudAPI.instance().templates().current().settings().maintenance() &&
                !LocalCache.get(WhitelistCache.class).contains(player.getName()) &&
                !player.hasPermission("pocketcloud.bypass.maintenance")) {
            event.setCancelReason(LanguageKey.INGAME_TEMPLATE_KICK_MAINTENANCE.translate());
            event.setCancelled();
            return;
        }

        PlayerConnectPacket.create(
                player.getName(),
                player.getAddress().getHostString(),
                player.getXuid(),
                player.getUniqueId(),
                player.getProtocol().getProtocol(),
                player.getProtocol().getMinecraftVersion()
        ).sendPacket();
    }

    public static void handle(PlayerDisconnectedEvent event) {
        ProxiedPlayer player = event.getPlayer();
        PlayerDisconnectPacket.create(player.getName()).sendPacket();

        if (!initialConnects.contains(player.getName())) {
            String reason = (LanguageKey.INGAME_TEMPLATE_KICK_MAINTENANCE.translate().equals(event.getReason()) ? "Template is in maintenance" : event.getReason());
            CloudBridge.instance().notifications().sendNotification(NotificationType.PLAYER_JOIN_FAILED, Map.of(
                    "player", player.getName(),
                    "server", CloudBridge.instance().environmentConfig().localServerName(),
                    "reason", reason
            ));
        } else initialConnects.remove(player.getName());
    }

    public static void handle(ServerTransferEvent event) {
        PlayerSwitchServerPacket.create(event.getPlayer().getName(), event.getTargetServer().getServerName()).sendPacket();
    }

    public static void handle(InitialServerDeterminedEvent event) {
        initialConnects.add(event.getPlayer().getName());
        PlayerSwitchServerPacket.create(event.getPlayer().getName(), event.getInitialServer().getServerName()).sendPacket();
    }
}