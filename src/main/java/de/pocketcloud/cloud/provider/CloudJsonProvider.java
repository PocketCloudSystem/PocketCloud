package de.pocketcloud.cloud.provider;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.cache.ActiveInGameModuleCache;
import de.pocketcloud.cloud.cache.LocalCache;
import de.pocketcloud.cloud.cache.NotificationListCache;
import de.pocketcloud.cloud.cache.WhitelistCache;
import de.pocketcloud.cloud.config.Config;
import de.pocketcloud.cloud.config.exception.UnsupportedFileExtensionException;
import de.pocketcloud.cloud.config.type.JsonConfigType;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.util.PocketCloudPaths;
import de.pocketcloud.cloud.util.concurrent.Promise;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class CloudJsonProvider extends CloudProvider {

    private Config templatesConfig;
    private Config serverGroupsConfig;
    private Config modulesConfig;
    private Config notificationsList;
    private Config maintenanceList;

    public CloudJsonProvider() {
        try {
            this.templatesConfig = new Config(PocketCloudPaths.templates().with("templates.json").asPath());
            this.serverGroupsConfig = new Config(PocketCloudPaths.groups().with("groups.json").asPath());
            this.modulesConfig = new Config(PocketCloudPaths.storage().inGame().with("modules.json").asPath(), new JsonConfigType(), Map.of(
                    ActiveInGameModuleCache.SIGN_MODULE, false,
                    ActiveInGameModuleCache.NPC_MODULE, false,
                    ActiveInGameModuleCache.HUB_COMMAND_MODULE, false
            ));

            this.notificationsList = new Config(PocketCloudPaths.storage().inGame().with("notificationList.json").asPath());
            this.maintenanceList = new Config(PocketCloudPaths.storage().inGame().with("maintenanceList.json").asPath());

            maintenanceList.getAll().keySet().stream().filter(p -> maintenanceList.get(p, false)).forEach(p -> LocalCache.get(WhitelistCache.class).add(p));
            notificationsList.getAll().keySet().stream().filter(p -> notificationsList.get(p, false)).forEach(p -> LocalCache.get(NotificationListCache.class).add(p));
            modulesConfig.getAll().keySet().stream().filter(m -> modulesConfig.get(m, false)).forEach(m -> LocalCache.get(ActiveInGameModuleCache.class).add(m));
        } catch (IOException | UnsupportedFileExtensionException e) {
            CloudLogger.get().exception("Unable to load CloudJsonProvider", e);
            PocketCloud.instance().shutdown();
        }
    }
    
    @Override
    public Promise<Void> addTemplate(Template template) {
        templatesConfig.set(template.name(), template.write());
        return saveConfig(templatesConfig);
    }

    @Override
    public Promise<Void> removeTemplate(Template template) {
        templatesConfig.remove(template.name());
        return saveConfig(templatesConfig);
    }

    @Override
    public Promise<Void> editTemplate(Template template, Map<String, Object> newData) {
        templatesConfig.set(template.name(), newData);
        return saveConfig(templatesConfig);
    }

    @Override
    public Promise<Optional<Template>> getTemplate(String templateName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = templatesConfig.get(templateName, Map.class);

        try {
            if (data == null) return Promise.resolved(Optional.empty());
            Template template = Template.read(data);
            return Promise.resolved(Optional.of(template));
        } catch (Exception e) {
            return Promise.failed(e);
        }
    }

    @Override
    public Promise<Boolean> checkTemplate(String template) {
        return Promise.resolved(templatesConfig.get(template, false));
    }

    @Override
    public Promise<Map<String, Template>> getTemplates() {
        Map<String, Template> templates = new HashMap<>();
        templatesConfig.getAll().forEach((_, data) -> {
            try {
                @SuppressWarnings("unchecked")
                Template template = Template.read((Map<String, Object>) data);
                templates.put(template.name(), template);
            } catch (Exception e) {
                CloudLogger.get().exception("Unable to load templates", e);
            }
        });
        return Promise.resolved(templates);
    }
    
    @Override
    public Promise<Void> addServerGroup(ServerGroup serverGroup) {
        serverGroupsConfig.set(serverGroup.name(), serverGroup.write());
        return saveConfig(serverGroupsConfig);
    }

    @Override
    public Promise<Void> removeServerGroup(ServerGroup serverGroup) {
        serverGroupsConfig.remove(serverGroup.name());
        return saveConfig(serverGroupsConfig);
    }

    @Override
    public Promise<Void> editServerGroup(ServerGroup serverGroup, Map<String, Object> newData) {
        serverGroupsConfig.set(serverGroup.name(), newData);
        return saveConfig(serverGroupsConfig);
    }

    @Override
    public Promise<Optional<ServerGroup>> getServerGroup(String serverGroupName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = serverGroupsConfig.get(serverGroupName, Map.class, null);

        try {
            if (data == null) return Promise.resolved(Optional.empty());
            ServerGroup group = ServerGroup.read(data);
            return Promise.resolved(Optional.of(group));
        } catch (Exception e) {
            return Promise.failed(e);
        }
    }

    @Override
    public Promise<Boolean> checkServerGroup(String serverGroup) {
        return Promise.resolved(serverGroupsConfig.has(serverGroup));
    }

    @Override
    public Promise<Map<String, ServerGroup>> getServerGroups() {
        Map<String, ServerGroup> serverGroups = new HashMap<>();
        serverGroupsConfig.getAll().forEach((_, data) -> {
            try {
                @SuppressWarnings("unchecked")
                ServerGroup group = ServerGroup.read((Map<String, Object>) data);
                serverGroups.put(group.name(), group);
            } catch (Exception _) {}
        });
        return Promise.resolved(serverGroups);
    }

    @Override
    public Promise<Void> setModuleState(String module, boolean enabled) {
        modulesConfig.set(module, enabled);
        LocalCache.get(ActiveInGameModuleCache.class).set(module, enabled);
        return saveConfig(modulesConfig);
    }

    @Override
    public Promise<Optional<Boolean>> getModuleState(String module) {
        return Promise.resolved(Optional.ofNullable(modulesConfig.get(module, null)));
    }

    @Override
    public Promise<Void> enablePlayerNotifications(String player) {
        notificationsList.set(player, true);
        LocalCache.get(NotificationListCache.class).add(player);
        return saveConfig(notificationsList);
    }

    @Override
    public Promise<Void> disablePlayerNotifications(String player) {
        notificationsList.remove(player);
        LocalCache.get(NotificationListCache.class).remove(player);
        return saveConfig(notificationsList);
    }

    @Override
    public Promise<Boolean> hasNotificationsEnabled(String player) {
        return Promise.resolved(notificationsList.get(player, false));
    }

    @Override
    public Promise<List<String>> getNotificationList() {
        return Promise.resolved(new ArrayList<>(notificationsList.getKeys()));
    }

    @Override
    public Promise<Void> addToWhitelist(String player) {
        maintenanceList.set(player, true);
        LocalCache.get(WhitelistCache.class).add(player);
        return saveConfig(notificationsList);
    }

    @Override
    public Promise<Void> removeFromWhitelist(String player) {
        maintenanceList.remove(player);
        LocalCache.get(WhitelistCache.class).remove(player);
        return saveConfig(notificationsList);
    }

    @Override
    public Promise<Boolean> isOnWhitelist(String player) {
        return Promise.resolved(maintenanceList.get(player, false));
    }

    @Override
    public Promise<List<String>> getWhitelist() {
        return Promise.resolved(maintenanceList.getAll().entrySet().stream()
                .filter(e -> Boolean.TRUE.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()));
    }

    private Promise<Void> saveConfig(Config config) {
        try {
            config.save();
            return Promise.resolved(null);
        } catch (Exception e) {
            return Promise.failed(e);
        }
    }
}