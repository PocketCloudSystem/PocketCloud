package de.pocketcloud.api.provider;

import de.pocketcloud.api.model.player.ICloudPlayer;
import de.pocketcloud.api.search.SearchQuery;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface IPlayerProvider<T extends ICloudPlayer> {

    void add(T player);

    void remove(T player);

    boolean check(String nameOrXuid);

    boolean check(UUID uuid);

    Optional<T> get(String nameOrXuid);

    Optional<T> get(UUID uuid);

    Collection<T> query(SearchQuery<? extends ICloudPlayer> searchQuery);

    Collection<T> getAll();
}