package de.pocketcloud.bridge.provider;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.provider.write.IWritePlayerProvider;
import de.pocketcloud.api.search.PlayerSearchQuery;
import de.pocketcloud.bridge.component.CloudPlayer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PlayerProvider implements IWritePlayerProvider {

    private final Map<String, ICloudPlayer> players = new ConcurrentHashMap<>();

    @Override
    public void add(ICloudPlayer player) {
        if (players.containsKey(player.name())) {
            ((CloudPlayer) players.get(player.name())).syncIn(player);
        } else players.put(player.name(), player);
    }

    @Override
    public void remove(ICloudPlayer player) {
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