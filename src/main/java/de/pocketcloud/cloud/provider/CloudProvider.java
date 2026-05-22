package de.pocketcloud.cloud.provider;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class CloudProvider {

    private static CloudProvider current = null;

    public abstract CompletableFuture<Void> addTemplate(Template template);

    public abstract CompletableFuture<Void> removeTemplate(Template template);

    public abstract CompletableFuture<Void> editTemplate(Template template, Map<String, Object> newData);

    public abstract CompletableFuture<Optional<Template>> getTemplate(String template);

    public abstract CompletableFuture<Boolean> checkTemplate(String template);

    public abstract CompletableFuture<Map<String, Template>> getTemplates();

    public abstract CompletableFuture<Void> addServerGroup(ServerGroup serverGroup);

    public abstract CompletableFuture<Void> removeServerGroup(ServerGroup serverGroup);

    public abstract CompletableFuture<Void> editServerGroup(ServerGroup serverGroup, Map<String, Object> newData);

    public abstract CompletableFuture<Optional<ServerGroup>> getServerGroup(String serverGroup);

    public abstract CompletableFuture<Boolean> checkServerGroup(String serverGroup);

    public abstract CompletableFuture<Map<String, ServerGroup>> getServerGroups();

    public abstract CompletableFuture<Void> setModuleState(String module, boolean enabled);

    public abstract CompletableFuture<Optional<Boolean>> getModuleState(String module);

    public abstract CompletableFuture<Void> enablePlayerNotifications(String player);

    public abstract CompletableFuture<Void> disablePlayerNotifications(String player);

    public abstract CompletableFuture<Boolean> hasNotificationsEnabled(String player);

    public abstract CompletableFuture<List<String>> getNotificationList();

    public abstract CompletableFuture<Void> addToWhitelist(String player);

    public abstract CompletableFuture<Void> removeFromWhitelist(String player);

    public abstract CompletableFuture<Boolean> isOnWhitelist(String player);

    public abstract CompletableFuture<List<String>> getWhitelist();

    public static void select() {
        String provider = PocketCloud.getInstance().config().provider();
        current = switch (provider) {
            case "mysql" -> new CloudMySqlProvider();
            default -> new CloudJsonProvider();
        };
    }

    public static CloudProvider current() {
        if (current == null) select();
        return current;
    }
}