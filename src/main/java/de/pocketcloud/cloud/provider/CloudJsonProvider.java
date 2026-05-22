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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
            PocketCloud.getInstance().shutdown();
        }
    }
    
    @Override
    public CompletableFuture<Void> addTemplate(Template template) {
        templatesConfig.set(template.name(), template.write());
        return saveConfig(templatesConfig);
    }

    @Override
    public CompletableFuture<Void> removeTemplate(Template template) {
        templatesConfig.remove(template.name());
        return saveConfig(templatesConfig);
    }

    @Override
    public CompletableFuture<Void> editTemplate(Template template, Map<String, Object> newData) {
        templatesConfig.set(template.name(), newData);
        return saveConfig(templatesConfig);
    }

    @Override
    public CompletableFuture<Optional<Template>> getTemplate(String templateName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = templatesConfig.get(templateName, Map.class);

        try {
            if (data == null) return CompletableFuture.completedFuture(Optional.empty());
            Template template = Template.read(data);
            return CompletableFuture.completedFuture(Optional.of(template));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Boolean> checkTemplate(String template) {
        return CompletableFuture.completedFuture(templatesConfig.get(template, false));
    }

    @Override
    public CompletableFuture<Map<String, Template>> getTemplates() {
        Map<String, Template> templates = new HashMap<>();
        templatesConfig.getAll().forEach((_, data) -> {
            try {
                Template template = Template.read((Map<String, Object>) data);
                templates.put(template.name(), template);
            } catch (Exception _) {}
        });
        return CompletableFuture.completedFuture(templates);
    }
    
    @Override
    public CompletableFuture<Void> addServerGroup(ServerGroup serverGroup) {
        serverGroupsConfig.set(serverGroup.name(), serverGroup.write());
        return saveConfig(serverGroupsConfig);
    }

    @Override
    public CompletableFuture<Void> removeServerGroup(ServerGroup serverGroup) {
        serverGroupsConfig.remove(serverGroup.name());
        return saveConfig(serverGroupsConfig);
    }

    @Override
    public CompletableFuture<Void> editServerGroup(ServerGroup serverGroup, Map<String, Object> newData) {
        serverGroupsConfig.set(serverGroup.name(), newData);
        return saveConfig(serverGroupsConfig);
    }

    @Override
    public CompletableFuture<Optional<ServerGroup>> getServerGroup(String serverGroupName) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = serverGroupsConfig.get(serverGroupName, Map.class, null);

        try {
            if (data == null) return CompletableFuture.completedFuture(Optional.empty());
            ServerGroup group = ServerGroup.read(data);
            return CompletableFuture.completedFuture(Optional.of(group));
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public CompletableFuture<Boolean> checkServerGroup(String serverGroup) {
        return CompletableFuture.completedFuture(serverGroupsConfig.has(serverGroup));
    }

    @Override
    public CompletableFuture<Map<String, ServerGroup>> getServerGroups() {
        Map<String, ServerGroup> serverGroups = new HashMap<>();
        serverGroupsConfig.getAll().forEach((_, data) -> {
            try {
                ServerGroup group = ServerGroup.read((Map<String, Object>) data);
                serverGroups.put(group.name(), group);
            } catch (Exception _) {}
        });
        return CompletableFuture.completedFuture(serverGroups);
    }

    @Override
    public CompletableFuture<Void> setModuleState(String module, boolean enabled) {
        modulesConfig.set(module, enabled);
        LocalCache.get(ActiveInGameModuleCache.class).set(module, enabled);
        return saveConfig(modulesConfig);
    }

    @Override
    public CompletableFuture<Optional<Boolean>> getModuleState(String module) {
        return CompletableFuture.completedFuture(Optional.ofNullable(modulesConfig.get(module, null)));
    }

    @Override
    public CompletableFuture<Void> enablePlayerNotifications(String player) {
        notificationsList.set(player, true);
        LocalCache.get(NotificationListCache.class).add(player);
        return saveConfig(notificationsList);
    }

    @Override
    public CompletableFuture<Void> disablePlayerNotifications(String player) {
        notificationsList.remove(player);
        LocalCache.get(NotificationListCache.class).remove(player);
        return saveConfig(notificationsList);
    }

    @Override
    public CompletableFuture<Boolean> hasNotificationsEnabled(String player) {
        return CompletableFuture.completedFuture(notificationsList.get(player, false));
    }

    @Override
    public CompletableFuture<List<String>> getNotificationList() {
        return CompletableFuture.completedFuture(new ArrayList<>(notificationsList.getKeys()));
    }

    @Override
    public CompletableFuture<Void> addToWhitelist(String player) {
        maintenanceList.set(player, true);
        LocalCache.get(WhitelistCache.class).add(player);
        return saveConfig(notificationsList);
    }

    @Override
    public CompletableFuture<Void> removeFromWhitelist(String player) {
        maintenanceList.remove(player);
        LocalCache.get(WhitelistCache.class).remove(player);
        return saveConfig(notificationsList);
    }

    @Override
    public CompletableFuture<Boolean> isOnWhitelist(String player) {
        return CompletableFuture.completedFuture(maintenanceList.get(player, false));
    }

    @Override
    public CompletableFuture<List<String>> getWhitelist() {
        return CompletableFuture.completedFuture(maintenanceList.getAll().entrySet().stream()
                .filter(e -> Boolean.TRUE.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()));
    }

    private CompletableFuture<Void> saveConfig(Config config) {
        try {
            config.save();
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}