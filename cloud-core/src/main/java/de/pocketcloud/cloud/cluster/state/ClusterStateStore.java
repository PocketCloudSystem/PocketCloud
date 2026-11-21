package de.pocketcloud.cloud.cluster.state;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Shared runtime state of the cloud (running servers, players, reserved ids, ...).
 * <p>
 * Today the only implementation is {@link LocalStateStore}, which keeps everything in memory.
 * In a cluster this is backed by Redis, so every node sees the same state. Values are plain
 * documents ({@code Map<String, Object>}, e.g. {@code ICloudServer.write()}) so they can be
 * stored as JSON by remote implementations.
 * <p>
 * Keys are logical names without prefix (see {@link StateKeys}); implementations add their own
 * namespace. Set operations must be atomic, because they are used for reservations.
 */
public interface ClusterStateStore {

    String name();

    void put(String hash, String field, Map<String, Object> document);

    Optional<Map<String, Object>> get(String hash, String field);

    Map<String, Map<String, Object>> getAll(String hash);

    void remove(String hash, String field);

    /**
     * Atomically adds a member to a set.
     *
     * @return true if the member was not present before
     */
    boolean addToSet(String set, String member);

    /**
     * @return true if the member was present
     */
    boolean removeFromSet(String set, String member);

    Set<String> members(String set);

    default void open() {}

    default void close() {}
}
