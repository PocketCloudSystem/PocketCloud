package de.pocketcloud.common.cache;

import java.util.Collection;
import java.util.List;

public interface LocalCache<T> {

    /**
     * Replaces local elements with elements {@param T cache}, mainly used for first time fetching (e.g. from db)
     */
    void syncIn(List<T> cache);

    /**
     * Pushes cache to sub-server instances
     */
    void syncOut();

    void add(T element);

    void remove(T element);

    boolean contains(T element);

    Collection<T> getAll();

    static <C extends LocalCache<?>> C get(Class<C> clazz) {
        return LocalCacheRegistry.get(clazz);
    }
}