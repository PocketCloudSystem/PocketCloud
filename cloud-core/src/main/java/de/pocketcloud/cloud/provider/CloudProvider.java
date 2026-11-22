package de.pocketcloud.cloud.provider;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.common.concurrent.Promise;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class CloudProvider {

    private static CloudProvider current = null;

    public abstract Promise<Void> addTemplate(ITemplate template);

    public abstract Promise<Void> removeTemplate(ITemplate template);

    public abstract Promise<Void> editTemplate(ITemplate template, Map<String, Object> newData);

    public abstract Promise<Optional<Template>> getTemplate(String template);

    public abstract Promise<Boolean> checkTemplate(String template);

    public abstract Promise<Map<String, Template>> getTemplates();

    public abstract Promise<Void> addServerGroup(IServerGroup serverGroup);

    public abstract Promise<Void> removeServerGroup(IServerGroup serverGroup);

    public abstract Promise<Void> editServerGroup(IServerGroup serverGroup, Map<String, Object> newData);

    public abstract Promise<Optional<ServerGroup>> getServerGroup(String serverGroup);

    public abstract Promise<Boolean> checkServerGroup(String serverGroup);

    public abstract Promise<Map<String, ServerGroup>> getServerGroups();

    public abstract Promise<Void> enablePlayerNotifications(String player);

    public abstract Promise<Void> disablePlayerNotifications(String player);

    public abstract Promise<Boolean> hasNotificationsEnabled(String player);

    public abstract Promise<List<String>> getNotificationList();

    public abstract Promise<Void> addToWhitelist(String player);

    public abstract Promise<Void> removeFromWhitelist(String player);

    public abstract Promise<Boolean> isOnWhitelist(String player);

    public abstract Promise<List<String>> getWhitelist();

    public static void select() {
        String provider = PocketCloud.instance().config().provider();
        CloudProvider persistent = switch (provider.toLowerCase()) {
            case "mysql" -> new CloudMySqlProvider();
            default -> new CloudJsonProvider();
        };

        CloudProvider cached = PocketCloud.instance().cluster().redisConnection()
                .<CloudProvider>map(redis -> new CloudRedisProvider(persistent, redis))
                .orElse(persistent);
        // Outermost: sends writes of non-head nodes to the head and tells all nodes about the head's writes.
        current = new de.pocketcloud.cloud.cluster.data.ClusterDataProvider(cached);
    }

    /** The provider this one wraps, or null for a persistent provider (JSON, MySQL). */
    public CloudProvider wrapped() {
        return null;
    }

    /** The persistent provider (JSON or MySQL) below the cluster and Redis layers. */
    public static CloudProvider persistent() {
        CloudProvider provider = current();
        while (provider.wrapped() != null) provider = provider.wrapped();
        return provider;
    }

    /** The Redis cache layer, if Redis is enabled. */
    public static java.util.Optional<CloudRedisProvider> redis() {
        CloudProvider provider = current();
        while (provider != null) {
            if (provider instanceof CloudRedisProvider redis) return java.util.Optional.of(redis);
            provider = provider.wrapped();
        }
        return java.util.Optional.empty();
    }

    public static CloudProvider current() {
        if (current == null) select();
        return current;
    }
}