package de.pocketcloud.api.provider;

import de.pocketcloud.api.component.player.ICloudPlayer;
import de.pocketcloud.api.search.PlayerSearchQuery;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface IPlayerProvider {

    boolean check(String nameOrXuid);

    boolean check(UUID uuid);

    Optional<ICloudPlayer> get(String nameOrXuid);

    Optional<ICloudPlayer> get(UUID uuid);

    Collection<ICloudPlayer> query(PlayerSearchQuery searchQuery);

    Collection<ICloudPlayer> query(Consumer<PlayerSearchQuery> queryConsumer);

    Collection<ICloudPlayer> getAll();
}