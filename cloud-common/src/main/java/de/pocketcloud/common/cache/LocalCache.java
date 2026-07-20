package de.pocketcloud.common.cache;

import java.util.Map;
import java.util.Optional;

public interface LocalCache<K, V> {

    void syncIn(Map<K, V> cache);

    void syncOut();

    void add(K id, V element);

    void remove(K id);

    void clear();

    boolean contains(K id);

    int size();

    Optional<V> get(K id);

    Map<K, V> getAll();

    static <C extends LocalCache<?, ?>> C get(Class<C> clazz) {
        return LocalCacheRegistry.get(clazz);
    }
}