package de.pocketcloud.cloud.cluster.state;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link ClusterStateStore} for single-machine setups. Nothing is shared or persisted.
 */
public final class LocalStateStore implements ClusterStateStore {

    private final Map<String, Map<String, Map<String, Object>>> hashes = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sets = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "local";
    }

    @Override
    public void put(String hash, String field, Map<String, Object> document) {
        hashes.computeIfAbsent(hash, _ -> new ConcurrentHashMap<>()).put(field, Map.copyOf(withoutNulls(document)));
    }

    @Override
    public Optional<Map<String, Object>> get(String hash, String field) {
        Map<String, Map<String, Object>> map = hashes.get(hash);
        return map == null ? Optional.empty() : Optional.ofNullable(map.get(field));
    }

    @Override
    public Map<String, Map<String, Object>> getAll(String hash) {
        Map<String, Map<String, Object>> map = hashes.get(hash);
        return map == null ? Map.of() : Map.copyOf(map);
    }

    @Override
    public void remove(String hash, String field) {
        hashes.computeIfPresent(hash, (_, map) -> {
            map.remove(field);
            return map.isEmpty() ? null : map;
        });
    }

    @Override
    public boolean addToSet(String set, String member) {
        return sets.computeIfAbsent(set, _ -> ConcurrentHashMap.newKeySet()).add(member);
    }

    @Override
    public boolean removeFromSet(String set, String member) {
        Set<String> s = sets.get(set);
        return s != null && s.remove(member);
    }

    @Override
    public Set<String> members(String set) {
        Set<String> s = sets.get(set);
        return s == null ? Set.of() : Collections.unmodifiableSet(Set.copyOf(s));
    }

    private static Map<String, Object> withoutNulls(Map<String, Object> document) {
        Map<String, Object> copy = new HashMap<>();
        document.forEach((key, value) -> {
            if (key != null && value != null) copy.put(key, value);
        });
        return copy;
    }
}
