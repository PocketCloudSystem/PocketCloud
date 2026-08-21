package de.pocketcloud.cloud.player;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.provider.write.IWritePlayerProvider;
import de.pocketcloud.api.search.PlayerSearchQuery;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.api.template.TemplateType;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.shared.event.player.PlayerJoinFailedEvent;
import de.pocketcloud.shared.event.player.PlayerJoinedEvent;
import de.pocketcloud.shared.event.player.PlayerLeftEvent;
import de.pocketcloud.shared.network.packet.type.NotificationType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class CloudPlayerManager implements IWritePlayerProvider {

    private final Map<String, CloudPlayer> players = new ConcurrentHashMap<>();

    @Override
    public void add(ICloudPlayer player) {
        CloudPlayer cloudPlayer = requireCloudPlayer(player);
        boolean anyProxies = !PocketCloud.instance().servers().query(ServerSearchQuery.create().ofType(TemplateType.PROXY)).isEmpty();
        if (anyProxies && cloudPlayer.currentProxy().isEmpty()) {
            cloudPlayer.kick("Joined via sub-server instead of a proxy.", "Please do not join via sub-servers.");
            CloudAPI.instance().events().call(new PlayerJoinFailedEvent(player, cloudPlayer.currentServer().orElse(null), "Joined via sub-server instead of a proxy."));
            return;
        }

        if (PocketCloud.instance().notifications().canLog(NotificationType.PLAYER_JOINED)) {
            String via = cloudPlayer.currentProxyName() != null ? cloudPlayer.currentProxyName() : cloudPlayer.currentServerName();
            CloudLogger.get().info("Player §b{} §rhas §aconnected §rvia §b{}§r.", cloudPlayer.name(), via);
        }

        players.put(cloudPlayer.name(), cloudPlayer);
        cloudPlayer.syncOut();
        String serverOrProxy = cloudPlayer.currentServerName() != null ? cloudPlayer.currentServerName() : cloudPlayer.currentProxyName();
        PocketCloud.instance().notifications().sendNotification(NotificationType.PLAYER_JOINED, Map.of("player", cloudPlayer.name(), "server", serverOrProxy), Map.of());

        var joinTarget = player.currentServer().orElse(player.currentProxy().orElse(null));
        CloudAPI.instance().events().call(new PlayerJoinedEvent(player, joinTarget));
    }

    @Override
    public void remove(ICloudPlayer player) {
        CloudPlayer cloudPlayer = requireCloudPlayer(player);
        if (PocketCloud.instance().notifications().canLog(NotificationType.PLAYER_JOINED)) {
            String from = player.currentServerName() != null ? player.currentServerName() : cloudPlayer.currentProxyName();
            CloudLogger.get().info("Player §b{} §cdisconnected §rfrom §b{}§r.", cloudPlayer.name(), from);
        }

        players.remove(cloudPlayer.name());

        String serverOrProxy = cloudPlayer.currentServerName() != null ? cloudPlayer.currentServerName() : cloudPlayer.currentProxyName();
        PocketCloud.instance().notifications().sendNotification(NotificationType.PLAYER_LEFT, Map.of("player", cloudPlayer.name(), "server", serverOrProxy), Map.of());

        var disconnectTarget = cloudPlayer.currentServer().orElse(cloudPlayer.currentProxy().orElse(null));
        CloudAPI.instance().events().call(new PlayerLeftEvent(player, disconnectTarget));

        cloudPlayer.resetCurrentServer();
        cloudPlayer.resetCurrentProxy();
        cloudPlayer.markForRemoval().syncOut();
    }

    @Override
    public boolean check(String nameOrXuid) {
        return players.containsKey(nameOrXuid) || players.values().stream().anyMatch(p -> p.xboxUserId().equals(nameOrXuid));
    }

    @Override
    public boolean check(UUID uuid) {
        return players.values().stream().anyMatch(p -> p.uniqueId().equals(uuid));
    }

    @Override
    public Optional<ICloudPlayer> get(String nameOrXuid) {
        if (players.containsKey(nameOrXuid)) return Optional.of(players.get(nameOrXuid));
        return widen(players.values()).stream()
                .filter(p -> p.xboxUserId().equals(nameOrXuid))
                .findFirst();
    }

    @Override
    public Optional<ICloudPlayer> get(UUID uuid) {
        return widen(players.values()).stream().filter(p -> p.uniqueId().equals(uuid)).findFirst();
    }

    @Override
    public Collection<ICloudPlayer> query(PlayerSearchQuery searchQuery) {
        return widen(players.values().stream()
                .filter(searchQuery::matches)
                .toList());
    }

    @Override
    public Collection<ICloudPlayer> query(Consumer<PlayerSearchQuery> queryConsumer) {
        PlayerSearchQuery searchQuery = new PlayerSearchQuery();
        queryConsumer.accept(searchQuery);
        return query(searchQuery);
    }

    @Override
    public int playerCount() {
        return players.size();
    }

    @Override
    public Collection<ICloudPlayer> getAll() {
        return widen(List.copyOf(players.values()));
    }

    @SuppressWarnings("unchecked")
    private <T extends ICloudPlayer> Collection<ICloudPlayer> widen(Collection<T> collection) {
        return (Collection<ICloudPlayer>) collection;
    }

    private CloudPlayer requireCloudPlayer(ICloudPlayer player) {
        if (!(player instanceof CloudPlayer cloudPlayer)) {
            throw new IllegalArgumentException("Unsupported ICloudServer implementation: " + player.getClass().getName());
        }

        return cloudPlayer;
    }
}