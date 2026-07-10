package de.pocketcloud.cloud.player;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.player.PlayerConnectEvent;
import de.pocketcloud.cloud.event.impl.player.PlayerDisconnectEvent;
import de.pocketcloud.cloud.notification.Notifier;
import de.pocketcloud.network.packet.type.NotificationType;
import de.pocketcloud.cloud.network.packet.impl.PlayerSyncPacket;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.CloudServerManager;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.TemplateType;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.util.FilterableObject;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.*;

public final class CloudPlayerManager {

    @Getter
    @Accessors(fluent = true)
    private static CloudPlayerManager instance;

    private final Map<String, CloudPlayer> players = new LinkedHashMap<>();

    public CloudPlayerManager() {
        instance = this;
    }

    public void add(CloudPlayer player) {
        boolean anyProxies = !CloudServerManager.instance().getAll(TemplateType.PROXY).isEmpty();
        if (anyProxies && player.currentProxy().isEmpty()) {
            player.kick("Joined via sub-server instead of a proxy.", "Please do not join via sub-servers.");
            return;
        }

        if (Notifier.canLog(NotificationType.PLAYER_JOINED)) {
            String via = player.currentProxyName() != null ? player.currentProxyName() : player.currentServerName();
            CloudLogger.get().info("Player §b{} §rhas §aconnected §rvia §b{}§r.", player.name(), via);
        }

        players.put(player.name(), player);
        PlayerSyncPacket.create(player, false).broadcastPacket();
        String serverOrProxy = player.currentServerName() != null ? player.currentServerName() : player.currentProxyName();
        Notifier.notify(NotificationType.PLAYER_JOINED, Map.of("player", player.name(), "server", serverOrProxy), Map.of());

        var joinTarget = player.currentServer().orElse(player.currentProxy().orElse(null));
        new PlayerConnectEvent(player, joinTarget).call();
    }

    public void remove(CloudPlayer player) {
        if (Notifier.canLog(NotificationType.PLAYER_JOINED)) {
            String from = player.currentServerName() != null ? player.currentServerName() : player.currentProxyName();
            CloudLogger.get().info("Player §b{} §cdisconnected §rfrom §b{}§r.", player.name(), from);
        }

        players.remove(player.name());

        String serverOrProxy = player.currentServerName() != null ? player.currentServerName() : player.currentProxyName();
        Notifier.notify(NotificationType.PLAYER_LEFT, Map.of("player", player.name(), "server", serverOrProxy), Map.of());

        var disconnectTarget = player.currentServer().orElse(player.currentProxy().orElse(null));
        new PlayerDisconnectEvent(player, disconnectTarget, serverOrProxy).call();

        player.setCurrentServer(null);
        player.setCurrentProxy(null);
        PlayerSyncPacket.create(player, true).broadcastPacket();
    }

    public Optional<CloudPlayer> get(String name) {
        if (players.containsKey(name)) return Optional.of(players.get(name));
        return players.values().stream()
            .filter(p -> p.xboxUserId().equals(name) || p.uniqueId().equals(name))
            .findFirst();
    }

    public List<CloudPlayer> getAll(FilterableObject object) {
        if (object == null) return new ArrayList<>(players.values());

        String objectName = switch (object) {
            case Template t -> t.name();
            case CloudServer s -> s.name();
            case ServerGroup g -> g.name();
            default -> throw new IllegalArgumentException("Unsupported filter type: " + object.getClass());
        };

        return players.values().stream().filter(player -> {
            if (objectName.equals(player.currentServerName())) return true;
            if (objectName.equals(player.currentProxyName()))  return true;
            var cs = player.currentServer().orElse(null);
            if (cs != null && objectName.equals(cs.templateName())) return true;
            if (cs != null && cs.template().isParentGroup(objectName)) return true;
            var cp = player.currentProxy().orElse(null);
            if (cp != null && objectName.equals(cp.templateName())) return true;
            return cp != null && cp.template().isParentGroup(objectName);
        }).toList();
    }

    public List<CloudPlayer> getAll() {
        return getAll(null);
    }
}
