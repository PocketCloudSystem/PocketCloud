package de.pocketcloud.bridge.cache;

import de.pocketcloud.api.sync.SyncingElement;
import de.pocketcloud.common.cache.LocalCache;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Local whitelist cache for the server
 */
public final class WhitelistCache implements LocalCache<String, Boolean>, SyncingElement<Map<String, Boolean>> {

    private final Map<String, Boolean> whitelist = new HashMap<>();

    @Override
    public void syncIn(Map<String, Boolean> cache) {
        whitelist.clear();
        whitelist.putAll(cache);
    }

    @Override
    public void syncOut() {}

    @Override
    public void add(String key, @NotNull Boolean value) {
        whitelist.put(key, value);
    }

    @Override
    public void remove(String element) {
        whitelist.remove(element);
    }

    @Override
    public void clear() {
        whitelist.clear();
    }

    @Override
    public boolean contains(String element) {
        return whitelist.containsKey(element);
    }

    @Override
    public int size() {
        return whitelist.size();
    }

    @Override
    public Optional<Boolean> get(String id) {
        return Optional.ofNullable(whitelist.get(id));
    }

    @Override
    public Map<String, Boolean> getAll() {
        return Collections.unmodifiableMap(whitelist);
    }
}