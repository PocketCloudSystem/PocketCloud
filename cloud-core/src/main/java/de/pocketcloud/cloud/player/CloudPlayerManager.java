package de.pocketcloud.cloud.player;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.component.server.ICloudServer;
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
    private final Map<String, CloudPlayer> playersByXuid = new ConcurrentHashMap<>();
    private final Map<UUID, CloudPlayer> playersByUuid = new ConcurrentHashMap<>();

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
        playersByXuid.put(cloudPlayer.xboxUserId(), cloudPlayer);
        playersByUuid.put(cloudPlayer.uniqueId(), cloudPlayer);
        cloudPlayer.syncOut();
        String serverOrProxy = cloudPlayer.currentServerName() != null ? cloudPlayer.currentServerName() : cloudPlayer.currentProxyName();
        PocketCloud.instance().notifications().sendNotification(NotificationType.PLAYER_JOINED, Map.of("player", cloudPlayer.name(), "server", serverOrProxy), Map.of());

        var joinTarget = player.currentServer().orElse(player.currentProxy().orElse(null));
        CloudAPI.instance().events().call(new PlayerJoinedEvent(player, joinTarget));
    }

    @Override
    public void remove(ICloudPlayer player) {
        CloudPlayer cloudPlayer = requireCloudPlayer(player);
        if (PocketCloud.instance().notifications().canLog(NotificationType.PLAYER_LEFT)) {
            String from = player.currentServerName() != null ? player.currentServerName() : cloudPlayer.currentProxyName();
            CloudLogger.get().info("Player §b{} §cdisconnected §rfrom §b{}§r.", cloudPlayer.name(), from);
        }

        players.remove(cloudPlayer.name(), cloudPlayer);
        playersByXuid.remove(cloudPlayer.xboxUserId(), cloudPlayer);
        playersByUuid.remove(cloudPlayer.uniqueId(), cloudPlayer);

        String serverOrProxy = cloudPlayer.currentServerName() != null ? cloudPlayer.currentServerName() : cloudPlayer.currentProxyName();
        PocketCloud.instance().notifications().sendNotification(NotificationType.PLAYER_LEFT, Map.of("player", cloudPlayer.name(), "server", serverOrProxy), Map.of());

        var disconnectTarget = cloudPlayer.currentServer().orElse(cloudPlayer.currentProxy().orElse(null));
        CloudAPI.instance().events().call(new PlayerLeftEvent(player, disconnectTarget));

        cloudPlayer.resetCurrentServer();
        cloudPlayer.resetCurrentProxy();
        cloudPlayer.markForRemoval().syncOut();
    }

    /**
     * Cleans up players that were connected to a server or proxy which is gone
     * (stopped, crashed, timed out). Without this, players of a crashed proxy stay
     * in the player list forever and their rejoin is ignored.
     */
    public void handleServerRemoved(ICloudServer server) {
        String name = server.name();
        for (CloudPlayer player : List.copyOf(players.values())) {
            if (name.equals(player.currentProxyName())) {
                remove(player);
            } else if (name.equals(player.currentServerName())) {
                if (player.currentProxyName() == null || player.currentProxy().isEmpty()) {
                    remove(player);
                } else {
                    player.resetCurrentServer();
                }
            }
        }
    }

    // ------------------------------------------------------------------ cluster

    /**
     * A player reported by another node (joined there, switched server, ...). Only the bridges of this
     * node are informed; the other node already published the change.
     */
    public void applyRemote(CloudPlayer incoming, String fromNode) {
        CloudPlayer existing = players.get(incoming.name());
        if (existing != null) {
            existing.applyRemote(incoming);
            existing.syncLocal();
            return;
        }

        players.put(incoming.name(), incoming);
        if (incoming.xboxUserId() != null) playersByXuid.put(incoming.xboxUserId(), incoming);
        if (incoming.uniqueId() != null) playersByUuid.put(incoming.uniqueId(), incoming);
        incoming.syncLocal();
        CloudLogger.get().debug("Player {} joined via {} on node {}", incoming.name(), incoming.currentProxyName(), fromNode);
    }

    /**
     * A player left on another node.
     */
    public void removeRemote(String name, String fromNode) {
        CloudPlayer existing = players.get(name);
        if (existing == null) return;
        removeSilently(existing);
        CloudLogger.get().debug("Player {} left on node {}", name, fromNode);
    }

    private void removeSilently(CloudPlayer player) {
        players.remove(player.name(), player);
        if (player.xboxUserId() != null) playersByXuid.remove(player.xboxUserId(), player);
        if (player.uniqueId() != null) playersByUuid.remove(player.uniqueId(), player);
        player.markForRemoval().syncLocal();
    }

    /**
     * The node a player belongs to: the node of its proxy, or of its server if it joined without a proxy.
     */
    public String ownerNode(ICloudPlayer player) {
        ICloudServer via = player.currentProxy().orElse(player.currentServer().orElse(null));
        return via == null ? PocketCloud.instance().cluster().nodeName() : PocketCloud.instance().cluster().nodeOf(via).name();
    }

    /** Players whose proxy (or server) runs on this node. */
    public Collection<CloudPlayer> ownedPlayers() {
        String self = PocketCloud.instance().cluster().nodeName();
        return players.values().stream().filter(p -> self.equals(ownerNode(p))).toList();
    }

    /**
     * Removes the players of a node that is gone (their connection to its proxy is lost anyway).
     * Every node does this itself, so nothing is published.
     */
    public int removePlayersOfNode(String node) {
        int removed = 0;
        for (CloudPlayer player : List.copyOf(players.values())) {
            if (node.equals(ownerNode(player))) {
                removeSilently(player);
                removed++;
            }
        }
        return removed;
    }

    /**
     * Whether an already known player entry is outdated when a new connect for that player arrives.
     * A connect via a proxy always starts a new session. Behind a proxy, a connect from a sub-server
     * is a normal server switch, unless the player's proxy no longer exists.
     */
    public boolean isStaleOnConnect(ICloudPlayer existing, ICloudServer connectedTo) {
        if (connectedTo.template().templateType().isProxy()) return true;
        if (existing.currentProxyName() == null) return true;
        return existing.currentProxy().isEmpty();
    }

    @Override
    public boolean check(String nameOrXuid) {
        return players.containsKey(nameOrXuid) || playersByXuid.containsKey(nameOrXuid);
    }

    @Override
    public boolean check(UUID uuid) {
        return playersByUuid.containsKey(uuid);
    }

    @Override
    public Optional<ICloudPlayer> get(String nameOrXuid) {
        CloudPlayer player = players.get(nameOrXuid);
        if (player == null) player = playersByXuid.get(nameOrXuid);
        return Optional.ofNullable(player);
    }

    @Override
    public Optional<ICloudPlayer> get(UUID uuid) {
        return Optional.ofNullable(playersByUuid.get(uuid));
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