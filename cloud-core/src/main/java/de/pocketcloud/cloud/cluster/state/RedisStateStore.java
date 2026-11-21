package de.pocketcloud.cloud.cluster.state;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.cloud.cluster.redis.RedisConnection;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.common.util.FileUtils;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ClusterStateStore} backed by Redis. Documents are stored as JSON in hashes,
 * sets map to Redis sets ({@code SADD} is atomic, which makes id reservations safe across nodes).
 * <p>
 * Every write also goes into a local in-memory mirror. When Redis is unreachable, reads fall back
 * to that mirror, so running servers and players keep working. Reservations ({@link #addToSet})
 * are not possible without Redis and fail, which blocks new server starts until Redis is back.
 */
public final class RedisStateStore implements ClusterStateStore {

    private static final Type DOCUMENT_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private final RedisConnection redis;
    private final LocalStateStore mirror = new LocalStateStore();
    private volatile boolean reachable = true;

    public RedisStateStore(RedisConnection redis) {
        this.redis = redis;
    }

    @Override
    public String name() {
        return "redis";
    }

    @Override
    public void put(String hash, String field, Map<String, Object> document) {
        mirror.put(hash, field, document);
        String json = FileUtils.GSON.toJson(withoutNulls(document));
        redis.async().hset(redis.key(hash), field, json).whenComplete((_, ex) -> handleAsync(ex, "write " + hash));
    }

    @Override
    public Optional<Map<String, Object>> get(String hash, String field) {
        try {
            String json = redis.sync().hget(redis.key(hash), field);
            markReachable();
            return json == null ? Optional.empty() : Optional.of(decode(json));
        } catch (RuntimeException e) {
            markUnreachable(e);
            return mirror.get(hash, field);
        }
    }

    @Override
    public Map<String, Map<String, Object>> getAll(String hash) {
        try {
            Map<String, String> raw = redis.sync().hgetall(redis.key(hash));
            markReachable();
            Map<String, Map<String, Object>> result = new HashMap<>();
            raw.forEach((field, json) -> result.put(field, decode(json)));
            return result;
        } catch (RuntimeException e) {
            markUnreachable(e);
            return mirror.getAll(hash);
        }
    }

    @Override
    public void remove(String hash, String field) {
        mirror.remove(hash, field);
        redis.async().hdel(redis.key(hash), field).whenComplete((_, ex) -> handleAsync(ex, "delete from " + hash));
    }

    @Override
    public boolean addToSet(String set, String member) {
        try {
            boolean added = redis.sync().sadd(redis.key(set), member) == 1;
            markReachable();
            if (added) mirror.addToSet(set, member);
            return added;
        } catch (RuntimeException e) {
            markUnreachable(e);
            throw new IllegalStateException("Redis is not reachable, cannot reserve " + member + " in " + set, e);
        }
    }

    @Override
    public boolean removeFromSet(String set, String member) {
        mirror.removeFromSet(set, member);
        try {
            boolean removed = redis.sync().srem(redis.key(set), member) == 1;
            markReachable();
            return removed;
        } catch (RuntimeException e) {
            markUnreachable(e);
            return false;
        }
    }

    @Override
    public Set<String> members(String set) {
        try {
            Set<String> members = redis.sync().smembers(redis.key(set));
            markReachable();
            return Set.copyOf(members);
        } catch (RuntimeException e) {
            markUnreachable(e);
            return mirror.members(set);
        }
    }

    public boolean reachable() {
        return reachable;
    }

    private void handleAsync(Throwable ex, String action) {
        if (ex == null) markReachable();
        else markUnreachable(ex);
    }

    private void markReachable() {
        if (!reachable) {
            reachable = true;
            CloudLogger.get().success("§aReconnected §rto §bRedis§r.");
        }
    }

    private void markUnreachable(Throwable ex) {
        if (reachable) {
            reachable = false;
            CloudLogger.get().error("§cLost connection §rto §bRedis§r: {}. Using the local state until it is back.", ex.getMessage());
        }
    }

    private static Map<String, Object> decode(String json) {
        return FileUtils.GSON.fromJson(json, DOCUMENT_TYPE);
    }

    private static Map<String, Object> withoutNulls(Map<String, Object> document) {
        Map<String, Object> copy = new HashMap<>();
        document.forEach((key, value) -> {
            if (key != null && value != null) copy.put(key, value);
        });
        return copy;
    }
}
