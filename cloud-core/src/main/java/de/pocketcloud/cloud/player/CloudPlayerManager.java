package de.pocketcloud.cloud.player;

import de.pocketcloud.api.model.player.ICloudPlayer;
import de.pocketcloud.api.provider.IPlayerProvider;
import de.pocketcloud.api.search.SearchQuery;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.player.PlayerConnectEvent;
import de.pocketcloud.cloud.event.impl.player.PlayerDisconnectEvent;
import de.pocketcloud.cloud.notification.Notifier;
import de.pocketcloud.network.packet.type.NotificationType;
import de.pocketcloud.cloud.network.packet.impl.PlayerSyncPacket;

import java.util.*;

public final class CloudPlayerManager implements IPlayerProvider<CloudPlayer> {

    private final Map<String, CloudPlayer> players = new LinkedHashMap<>();

    public void add(CloudPlayer player) {
        boolean anyProxies = !PocketCloud.instance().servers().query(ServerSearchQuery.create().ofType(TemplateType.PROXY)).isEmpty();
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

    @Override
    public boolean check(String nameOrXuid) {
        return players.containsKey(nameOrXuid) || players.values().stream().anyMatch(p -> p.xboxUserId().equals(nameOrXuid));
    }

    @Override
    public boolean check(UUID uuid) {
        return players.values().stream().anyMatch(p -> p.uniqueId().equals(uuid));
    }

    public Optional<CloudPlayer> get(String nameOrXuid) {
        if (players.containsKey(nameOrXuid)) return Optional.of(players.get(nameOrXuid));
        return players.values().stream()
            .filter(p -> p.xboxUserId().equals(nameOrXuid))
            .findFirst();
    }

    @Override
    public Optional<CloudPlayer> get(UUID uuid) {
        return players.values().stream().filter(p -> p.uniqueId().equals(uuid)).findFirst();
    }

    @Override
    public Collection<CloudPlayer> query(SearchQuery<? extends ICloudPlayer> searchQuery) {
        return filter(searchQuery);
    }

    @SuppressWarnings("unchecked")
    private <T extends ICloudPlayer> Collection<CloudPlayer> filter(SearchQuery<T> searchQuery) {
        return players.values().stream()
                .filter(o -> searchQuery.matches((T) o))
                .toList();
    }

    public Collection<CloudPlayer> getAll() {
        return players.values().stream().toList();
    }
}