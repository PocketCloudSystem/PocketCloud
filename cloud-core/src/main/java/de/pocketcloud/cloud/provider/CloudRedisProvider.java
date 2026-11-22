package de.pocketcloud.cloud.provider;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cluster.redis.RedisConnection;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.common.concurrent.Promise;
import de.pocketcloud.common.util.FileUtils;

import java.lang.reflect.Type;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Write-through cache in front of the persistent provider (JSON or MySQL).
 * <p>
 * The persistent provider stays the source of truth: every write goes there first and is then
 * mirrored into Redis, so all nodes can read templates, groups, the whitelist and the
 * notification list from Redis. Only the head writes to Redis (other nodes forward their writes to the head,
 * see {@link de.pocketcloud.cloud.cluster.data.ClusterDataProvider}). When a node becomes head it loads the full
 * data set from the persistent provider into Redis. If Redis fails, reads fall back to the persistent provider.
 */
public final class CloudRedisProvider extends CloudProvider {

    private static final Type DOCUMENT_TYPE = new TypeToken<Map<String, Object>>() {}.getType();

    private static final String TEMPLATES = "templates";
    private static final String GROUPS = "groups";
    private static final String WHITELIST = "whitelist";
    private static final String NOTIFICATIONS = "notifications";

    private final CloudProvider delegate;
    private final RedisConnection redis;

    public CloudRedisProvider(CloudProvider delegate, RedisConnection redis) {
        this.delegate = delegate;
        this.redis = redis;
        if (writesRedis()) copyListsToRedis();
    }

    public CloudProvider delegate() {
        return delegate;
    }

    @Override
    public CloudProvider wrapped() {
        return delegate;
    }

    /**
     * Only the head writes to Redis (spec: the head writes to the provider first and then to Redis).
     * Other nodes forward their writes to the head; a single node is always head.
     */
    private static boolean writesRedis() {
        return PocketCloud.instance().cluster().isHead();
    }

    /** Called when this node became head: Redis gets the full data set of the persistent provider. */
    public void fillFromProvider() {
        if (!writesRedis()) return;
        copyListsToRedis();
        refillTemplatesAndGroups();
    }

    /** Replaces the templates and groups in Redis with the ones of the persistent provider (head only). */
    public void refillTemplatesAndGroups() {
        if (!writesRedis()) return;
        delegate.getTemplates().thenSuccess(templates -> replaceHash(TEMPLATES, documents(templates.values(), Template::name, Template::write)));
        delegate.getServerGroups().thenSuccess(groups -> replaceHash(GROUPS, documents(groups.values(), ServerGroup::name, ServerGroup::write)));
    }

    private static <T> Map<String, Map<String, Object>> documents(Collection<T> values, Function<T, String> name, Function<T, Map<String, Object>> write) {
        Map<String, Map<String, Object>> documents = new HashMap<>();
        for (T value : values) documents.put(name.apply(value), write.apply(value));
        return documents;
    }

    // ---------------------------------------------------------------- templates
    // Every write goes to the persistent provider first and then (head only) to Redis.

    @Override
    public Promise<Void> addTemplate(ITemplate template) {
        Map<String, Object> document = template.write();
        return delegate.addTemplate(template).thenApply(v -> {
            cacheDocument(TEMPLATES, template.name(), document);
            return v;
        });
    }

    @Override
    public Promise<Void> removeTemplate(ITemplate template) {
        return delegate.removeTemplate(template).thenApply(v -> {
            uncache(TEMPLATES, template.name());
            return v;
        });
    }

    @Override
    public Promise<Void> editTemplate(ITemplate template, Map<String, Object> newData) {
        // The MySQL provider encodes the settings inside newData, so Redis gets a copy taken before.
        Map<String, Object> document = new HashMap<>(newData);
        return delegate.editTemplate(template, newData).thenApply(v -> {
            cacheDocument(TEMPLATES, template.name(), document);
            return v;
        });
    }

    @Override
    public Promise<Optional<Template>> getTemplate(String template) {
        return readCached(TEMPLATES, template, Template::read, () -> delegate.getTemplate(template));
    }

    @Override
    public Promise<Boolean> checkTemplate(String template) {
        return exists(TEMPLATES, template, () -> delegate.checkTemplate(template));
    }

    @Override
    public Promise<Map<String, Template>> getTemplates() {
        return delegate.getTemplates().thenApply(templates -> {
            if (writesRedis()) replaceHash(TEMPLATES, documents(templates.values(), Template::name, Template::write));
            return templates;
        });
    }

    // ---------------------------------------------------------------- groups

    @Override
    public Promise<Void> addServerGroup(IServerGroup serverGroup) {
        Map<String, Object> document = serverGroup.write();
        return delegate.addServerGroup(serverGroup).thenApply(v -> {
            cacheDocument(GROUPS, serverGroup.name(), document);
            return v;
        });
    }

    @Override
    public Promise<Void> removeServerGroup(IServerGroup serverGroup) {
        return delegate.removeServerGroup(serverGroup).thenApply(v -> {
            uncache(GROUPS, serverGroup.name());
            return v;
        });
    }

    @Override
    public Promise<Void> editServerGroup(IServerGroup serverGroup, Map<String, Object> newData) {
        Map<String, Object> document = new HashMap<>(newData);
        return delegate.editServerGroup(serverGroup, newData).thenApply(v -> {
            cacheDocument(GROUPS, serverGroup.name(), document);
            return v;
        });
    }

    @Override
    public Promise<Optional<ServerGroup>> getServerGroup(String serverGroup) {
        return readCached(GROUPS, serverGroup, ServerGroup::read, () -> delegate.getServerGroup(serverGroup));
    }

    @Override
    public Promise<Boolean> checkServerGroup(String serverGroup) {
        return exists(GROUPS, serverGroup, () -> delegate.checkServerGroup(serverGroup));
    }

    @Override
    public Promise<Map<String, ServerGroup>> getServerGroups() {
        return delegate.getServerGroups().thenApply(groups -> {
            if (writesRedis()) replaceHash(GROUPS, documents(groups.values(), ServerGroup::name, ServerGroup::write));
            return groups;
        });
    }

    // ---------------------------------------------------------------- notifications & whitelist

    @Override
    public Promise<Void> enablePlayerNotifications(String player) {
        return delegate.enablePlayerNotifications(player).thenApply(v -> {
            setMember(NOTIFICATIONS, player, true);
            return v;
        });
    }

    @Override
    public Promise<Void> disablePlayerNotifications(String player) {
        return delegate.disablePlayerNotifications(player).thenApply(v -> {
            setMember(NOTIFICATIONS, player, false);
            return v;
        });
    }

    @Override
    public Promise<Boolean> hasNotificationsEnabled(String player) {
        return isMember(NOTIFICATIONS, player, () -> delegate.hasNotificationsEnabled(player));
    }

    @Override
    public Promise<List<String>> getNotificationList() {
        return members(NOTIFICATIONS, delegate::getNotificationList);
    }

    @Override
    public Promise<Void> addToWhitelist(String player) {
        return delegate.addToWhitelist(player).thenApply(v -> {
            setMember(WHITELIST, player, true);
            return v;
        });
    }

    @Override
    public Promise<Void> removeFromWhitelist(String player) {
        return delegate.removeFromWhitelist(player).thenApply(v -> {
            setMember(WHITELIST, player, false);
            return v;
        });
    }

    @Override
    public Promise<Boolean> isOnWhitelist(String player) {
        return isMember(WHITELIST, player, () -> delegate.isOnWhitelist(player));
    }

    @Override
    public Promise<List<String>> getWhitelist() {
        return members(WHITELIST, delegate::getWhitelist);
    }

    // ---------------------------------------------------------------- helpers
    // Writes are synchronous, so they are done before the head tells the other nodes about the change.

    private void copyListsToRedis() {
        delegate.getWhitelist().thenSuccess(list -> replaceSet(WHITELIST, list));
        delegate.getNotificationList().thenSuccess(list -> replaceSet(NOTIFICATIONS, list));
    }

    private void cacheDocument(String hash, String field, Map<String, Object> document) {
        if (!writesRedis()) return;
        try {
            redis.sync().hset(redis.key(hash), field, FileUtils.GSON.toJson(document));
        } catch (RuntimeException e) {
            logFailure(e, "cache " + hash);
        }
    }

    private void uncache(String hash, String field) {
        if (!writesRedis()) return;
        try {
            redis.sync().hdel(redis.key(hash), field);
        } catch (RuntimeException e) {
            logFailure(e, "uncache " + hash);
        }
    }

    private void replaceHash(String hash, Map<String, Map<String, Object>> documents) {
        try {
            String key = redis.key(hash);
            redis.sync().del(key);
            if (documents.isEmpty()) return;
            Map<String, String> encoded = new HashMap<>();
            documents.forEach((field, doc) -> encoded.put(field, FileUtils.GSON.toJson(doc)));
            redis.sync().hset(key, encoded);
        } catch (RuntimeException e) {
            logFailure(e, "fill " + hash);
        }
    }

    private void replaceSet(String set, Collection<String> members) {
        try {
            String key = redis.key(set);
            redis.sync().del(key);
            if (!members.isEmpty()) redis.sync().sadd(key, members.toArray(String[]::new));
        } catch (RuntimeException e) {
            logFailure(e, "fill " + set);
        }
    }

    private void setMember(String set, String member, boolean present) {
        if (!writesRedis()) return;
        try {
            String key = redis.key(set);
            if (present) redis.sync().sadd(key, member);
            else redis.sync().srem(key, member);
        } catch (RuntimeException e) {
            logFailure(e, "update " + set);
        }
    }

    private <T> Promise<Optional<T>> readCached(String hash, String field, Function<Map<String, Object>, T> reader, Supplier<Promise<Optional<T>>> fallback) {
        try {
            String json = redis.sync().hget(redis.key(hash), field);
            if (json != null) {
                Map<String, Object> document = FileUtils.GSON.fromJson(json, DOCUMENT_TYPE);
                return Promise.resolved(Optional.of(reader.apply(document)));
            }
        } catch (RuntimeException e) {
            logFailure(e, "read " + hash);
        }
        return fallback.get();
    }

    private Promise<Boolean> exists(String hash, String field, Supplier<Promise<Boolean>> fallback) {
        try {
            return Promise.resolved(redis.sync().hexists(redis.key(hash), field));
        } catch (RuntimeException e) {
            logFailure(e, "read " + hash);
            return fallback.get();
        }
    }

    private Promise<Boolean> isMember(String set, String member, Supplier<Promise<Boolean>> fallback) {
        try {
            return Promise.resolved(redis.sync().sismember(redis.key(set), member));
        } catch (RuntimeException e) {
            logFailure(e, "read " + set);
            return fallback.get();
        }
    }

    private Promise<List<String>> members(String set, Supplier<Promise<List<String>>> fallback) {
        try {
            return Promise.resolved(new ArrayList<>(redis.sync().smembers(redis.key(set))));
        } catch (RuntimeException e) {
            logFailure(e, "read " + set);
            return fallback.get();
        }
    }

    private static void logFailure(Throwable ex, String action) {
        if (ex == null) return;
        CloudLogger.get().warn("Could not use the §bRedis §rcache§8: §c{}", ex.getMessage());
        CloudLogger.get().debug("Redis cache action: {}", action);
    }
}
