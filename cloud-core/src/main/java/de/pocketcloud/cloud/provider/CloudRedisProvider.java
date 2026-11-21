package de.pocketcloud.cloud.provider;

import com.google.gson.reflect.TypeToken;
import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
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
 * The persistent provider stays the source of truth: every write goes there first and is
 * mirrored into Redis, so all nodes can read templates, groups, the whitelist and the
 * notification list from Redis. On startup the full data set is loaded from the persistent
 * provider and replaces the Redis copy. If Redis fails, reads fall back to the persistent provider.
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
        copyListsToRedis();
    }

    public CloudProvider delegate() {
        return delegate;
    }

    // ---------------------------------------------------------------- templates

    @Override
    public Promise<Void> addTemplate(ITemplate template) {
        cacheDocument(TEMPLATES, template.name(), template.write());
        return delegate.addTemplate(template);
    }

    @Override
    public Promise<Void> removeTemplate(ITemplate template) {
        uncache(TEMPLATES, template.name());
        return delegate.removeTemplate(template);
    }

    @Override
    public Promise<Void> editTemplate(ITemplate template, Map<String, Object> newData) {
        cacheDocument(TEMPLATES, template.name(), newData);
        return delegate.editTemplate(template, newData);
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
            replaceHash(TEMPLATES, templates.values().stream().collect(HashMap::new, (m, t) -> m.put(t.name(), t.write()), HashMap::putAll));
            return templates;
        });
    }

    // ---------------------------------------------------------------- groups

    @Override
    public Promise<Void> addServerGroup(IServerGroup serverGroup) {
        cacheDocument(GROUPS, serverGroup.name(), serverGroup.write());
        return delegate.addServerGroup(serverGroup);
    }

    @Override
    public Promise<Void> removeServerGroup(IServerGroup serverGroup) {
        uncache(GROUPS, serverGroup.name());
        return delegate.removeServerGroup(serverGroup);
    }

    @Override
    public Promise<Void> editServerGroup(IServerGroup serverGroup, Map<String, Object> newData) {
        cacheDocument(GROUPS, serverGroup.name(), newData);
        return delegate.editServerGroup(serverGroup, newData);
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
            replaceHash(GROUPS, groups.values().stream().collect(HashMap::new, (m, g) -> m.put(g.name(), g.write()), HashMap::putAll));
            return groups;
        });
    }

    // ---------------------------------------------------------------- notifications & whitelist

    @Override
    public Promise<Void> enablePlayerNotifications(String player) {
        setMember(NOTIFICATIONS, player, true);
        return delegate.enablePlayerNotifications(player);
    }

    @Override
    public Promise<Void> disablePlayerNotifications(String player) {
        setMember(NOTIFICATIONS, player, false);
        return delegate.disablePlayerNotifications(player);
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
        setMember(WHITELIST, player, true);
        return delegate.addToWhitelist(player);
    }

    @Override
    public Promise<Void> removeFromWhitelist(String player) {
        setMember(WHITELIST, player, false);
        return delegate.removeFromWhitelist(player);
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

    private void copyListsToRedis() {
        delegate.getWhitelist().thenSuccess(list -> replaceSet(WHITELIST, list));
        delegate.getNotificationList().thenSuccess(list -> replaceSet(NOTIFICATIONS, list));
    }

    private void cacheDocument(String hash, String field, Map<String, Object> document) {
        String json = FileUtils.GSON.toJson(document);
        redis.async().hset(redis.key(hash), field, json).whenComplete((_, ex) -> logFailure(ex, "cache " + hash));
    }

    private void uncache(String hash, String field) {
        redis.async().hdel(redis.key(hash), field).whenComplete((_, ex) -> logFailure(ex, "uncache " + hash));
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
        String key = redis.key(set);
        (present ? redis.async().sadd(key, member) : redis.async().srem(key, member))
                .whenComplete((_, ex) -> logFailure(ex, "update " + set));
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
        if (ex != null) CloudLogger.get().debug("Redis provider cache could not " + action + ": " + ex.getMessage());
    }
}
