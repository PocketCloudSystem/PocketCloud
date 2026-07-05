package de.pocketcloud.cloud.provider;

import de.pocketcloud.cloud.config.MainConfig;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.util.concurrent.Promise;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class CloudProvider {

    private static CloudProvider current = null;

    public abstract Promise<Void> addTemplate(Template template);

    public abstract Promise<Void> removeTemplate(Template template);

    public abstract Promise<Void> editTemplate(Template template, Map<String, Object> newData);

    public abstract Promise<Optional<Template>> getTemplate(String template);

    public abstract Promise<Boolean> checkTemplate(String template);

    public abstract Promise<Map<String, Template>> getTemplates();

    public abstract Promise<Void> addServerGroup(ServerGroup serverGroup);

    public abstract Promise<Void> removeServerGroup(ServerGroup serverGroup);

    public abstract Promise<Void> editServerGroup(ServerGroup serverGroup, Map<String, Object> newData);

    public abstract Promise<Optional<ServerGroup>> getServerGroup(String serverGroup);

    public abstract Promise<Boolean> checkServerGroup(String serverGroup);

    public abstract Promise<Map<String, ServerGroup>> getServerGroups();

    public abstract Promise<Void> setModuleState(String module, boolean enabled);

    public abstract Promise<Optional<Boolean>> getModuleState(String module);

    public abstract Promise<Void> enablePlayerNotifications(String player);

    public abstract Promise<Void> disablePlayerNotifications(String player);

    public abstract Promise<Boolean> hasNotificationsEnabled(String player);

    public abstract Promise<List<String>> getNotificationList();

    public abstract Promise<Void> addToWhitelist(String player);

    public abstract Promise<Void> removeFromWhitelist(String player);

    public abstract Promise<Boolean> isOnWhitelist(String player);

    public abstract Promise<List<String>> getWhitelist();

    public static void select() {
        String provider = MainConfig.instance().getProvider();
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