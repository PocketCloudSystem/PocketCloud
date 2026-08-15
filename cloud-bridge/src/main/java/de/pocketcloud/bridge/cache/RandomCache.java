package de.pocketcloud.bridge.cache;

import de.pocketcloud.common.cache.LocalCache;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RandomCache implements LocalCache<String, Object> {

    public static final String KEY_LAST_KEEP_ALIVE = "last_keep_alive";

    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    @Override
    public void syncIn(Map<String, Object> cache) {}

    @Override
    public void syncOut() {}

    @Override
    public void add(String id, Object value) {
        cache.put(id, value);
    }

    @Override
    public void remove(String id) {
        cache.remove(id);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public boolean contains(String id) {
        return cache.containsKey(id);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public Optional<Object> get(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String id, Class<T> type) {
        return Optional.ofNullable((T) cache.get(id));
    }

    @Override
    public Map<String, Object> getAll() {
        return Collections.unmodifiableMap(cache);
    }
}