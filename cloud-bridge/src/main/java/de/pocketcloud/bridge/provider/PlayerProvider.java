package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.provider.write.IWritePlayerProvider;
import de.pocketcloud.api.search.PlayerSearchQuery;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.component.CloudPlayer;
import de.pocketcloud.shared.event.player.PlayerJoinedEvent;
import de.pocketcloud.shared.event.player.PlayerLeftEvent;
import de.pocketcloud.shared.event.player.PlayerTransferredEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PlayerProvider implements IWritePlayerProvider {

    private final Map<String, ICloudPlayer> players = new ConcurrentHashMap<>();

    @Override
    public void add(ICloudPlayer player) {
        boolean verified = CloudBridge.instance().status().isVerified();
        if (players.containsKey(player.name())) {
            CloudPlayer localPlayer = (CloudPlayer) players.get(player.name());
            String oldServerName = localPlayer.currentServerName();
            localPlayer.syncIn(player);
            if (verified && !Objects.equals(oldServerName, localPlayer.currentServerName()))
                CloudAPI.instance().events().call(new PlayerTransferredEvent(localPlayer, oldServerName == null ? null : CloudAPI.instance().servers().get(oldServerName).orElse(null), localPlayer.currentServer().orElse(null)));
        } else {
            players.put(player.name(), player);
            if (verified)
                CloudAPI.instance().events().call(new PlayerJoinedEvent(player, player.currentServer().orElse(null)));
        }
    }

    @Override
    public void remove(ICloudPlayer player) {
        CloudPlayer localPlayer = (CloudPlayer) players.get(player.name());
        if (CloudBridge.instance().status().isVerified())
            CloudAPI.instance().events().call(new PlayerLeftEvent(localPlayer, localPlayer.currentServer().orElse(null)));
        players.remove(player.name());
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
        return Optional.ofNullable(players.getOrDefault(nameOrXuid, players.values().stream().filter(p -> p.xboxUserId().equals(nameOrXuid)).findFirst().orElse(null)));
    }

    @Override
    public Optional<ICloudPlayer> get(UUID uuid) {
        return players.values().stream().filter(p -> p.uniqueId().equals(uuid)).findFirst();
    }

    @Override
    public Collection<ICloudPlayer> query(PlayerSearchQuery searchQuery) {
        return players.values().stream()
                .filter(searchQuery::matches)
                .toList();
    }

    @Override
    public Collection<ICloudPlayer> query(Consumer<PlayerSearchQuery> queryConsumer) {
        PlayerSearchQuery searchQuery = new PlayerSearchQuery();
        queryConsumer.accept(searchQuery);
        return query(searchQuery);
    }

    @Override
    public Collection<ICloudPlayer> getAll() {
        return players.values().stream().toList();
    }
}