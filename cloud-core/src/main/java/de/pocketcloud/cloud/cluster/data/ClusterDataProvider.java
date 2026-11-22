package de.pocketcloud.cloud.cluster.data;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.common.concurrent.Promise;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Outermost provider layer (cluster spec: "TemplateManager / ServerGroupManager Schreiben: auf Nicht-Heads an den
 * Head weiterleiten", "Der Head schreibt immer zuerst in den Provider und danach nach Redis").
 * <ul>
 *     <li>Whitelist and notification writes of a node that is not head are sent to the head
 *     ({@link ClusterDataModule#forward}); every caller (console, HTTP, bridge packets, plugins) is covered.</li>
 *     <li>On the head (and on a single node) writes go to the provider below (Redis layer → JSON/MySQL) and
 *     then every other node is told about the change, so it updates its in-memory state.</li>
 *     <li>Template and group writes come from the {@code TemplateManager}/{@code ServerGroupManager}, which forward
 *     to the head themselves; writes that still reach this layer on a non-head node (start-up fixes of the local
 *     files) pass through without being announced.</li>
 * </ul>
 * Reads always pass through.
 */
public final class ClusterDataProvider extends CloudProvider {

    private final CloudProvider delegate;

    public ClusterDataProvider(CloudProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public CloudProvider wrapped() {
        return delegate;
    }

    private static Optional<ClusterDataModule> module() {
        return ClusterDataModule.get();
    }

    /** The module if this node writes as head of a cluster (and must announce its writes). */
    private static Optional<ClusterDataModule> announcing() {
        return module().filter(ClusterDataModule::isHead);
    }

    /** The module if this node is a cluster member that is not head (and must forward its writes). */
    private static Optional<ClusterDataModule> forwarding() {
        return module().filter(m -> !m.isHead());
    }

    // ---------------------------------------------------------------- templates

    @Override
    public Promise<Void> addTemplate(ITemplate template) {
        Map<String, Object> document = template.write();
        return announce(delegate.addTemplate(template), ClusterDataModule.KIND_TEMPLATE, template.name(), document);
    }

    @Override
    public Promise<Void> removeTemplate(ITemplate template) {
        return announce(delegate.removeTemplate(template), ClusterDataModule.KIND_TEMPLATE, template.name(), null);
    }

    @Override
    public Promise<Void> editTemplate(ITemplate template, Map<String, Object> newData) {
        Map<String, Object> document = new HashMap<>(newData);
        return announce(delegate.editTemplate(template, newData), ClusterDataModule.KIND_TEMPLATE, template.name(), document);
    }

    @Override
    public Promise<Optional<Template>> getTemplate(String template) {
        return delegate.getTemplate(template);
    }

    @Override
    public Promise<Boolean> checkTemplate(String template) {
        return delegate.checkTemplate(template);
    }

    @Override
    public Promise<Map<String, Template>> getTemplates() {
        return delegate.getTemplates();
    }

    // ---------------------------------------------------------------- groups

    @Override
    public Promise<Void> addServerGroup(IServerGroup serverGroup) {
        Map<String, Object> document = serverGroup.write();
        return announce(delegate.addServerGroup(serverGroup), ClusterDataModule.KIND_GROUP, serverGroup.name(), document);
    }

    @Override
    public Promise<Void> removeServerGroup(IServerGroup serverGroup) {
        return announce(delegate.removeServerGroup(serverGroup), ClusterDataModule.KIND_GROUP, serverGroup.name(), null);
    }

    @Override
    public Promise<Void> editServerGroup(IServerGroup serverGroup, Map<String, Object> newData) {
        Map<String, Object> document = new HashMap<>(newData);
        return announce(delegate.editServerGroup(serverGroup, newData), ClusterDataModule.KIND_GROUP, serverGroup.name(), document);
    }

    @Override
    public Promise<Optional<ServerGroup>> getServerGroup(String serverGroup) {
        return delegate.getServerGroup(serverGroup);
    }

    @Override
    public Promise<Boolean> checkServerGroup(String serverGroup) {
        return delegate.checkServerGroup(serverGroup);
    }

    @Override
    public Promise<Map<String, ServerGroup>> getServerGroups() {
        return delegate.getServerGroups();
    }

    // ---------------------------------------------------------------- notifications & whitelist

    @Override
    public Promise<Void> enablePlayerNotifications(String player) {
        return playerList(ClusterDataModule.KIND_NOTIFICATIONS, player, true);
    }

    @Override
    public Promise<Void> disablePlayerNotifications(String player) {
        return playerList(ClusterDataModule.KIND_NOTIFICATIONS, player, false);
    }

    @Override
    public Promise<Boolean> hasNotificationsEnabled(String player) {
        return delegate.hasNotificationsEnabled(player);
    }

    @Override
    public Promise<List<String>> getNotificationList() {
        return delegate.getNotificationList();
    }

    @Override
    public Promise<Void> addToWhitelist(String player) {
        return playerList(ClusterDataModule.KIND_WHITELIST, player, true);
    }

    @Override
    public Promise<Void> removeFromWhitelist(String player) {
        return playerList(ClusterDataModule.KIND_WHITELIST, player, false);
    }

    @Override
    public Promise<Boolean> isOnWhitelist(String player) {
        return delegate.isOnWhitelist(player);
    }

    @Override
    public Promise<List<String>> getWhitelist() {
        return delegate.getWhitelist();
    }

    // ---------------------------------------------------------------- helpers

    private Promise<Void> playerList(String kind, String player, boolean present) {
        Optional<ClusterDataModule> forward = forwarding();
        if (forward.isPresent()) {
            String operation = (present ? "+" : "-") + kind;
            return forward.get().forward(operation, player, null)
                    .failure(ex -> CloudLogger.get().error("§cCould not change the {} for §b{} §con the head§r: {}", kind, player, ex.getMessage()))
                    .thenApply(_ -> null);
        }

        Promise<Void> write = switch (kind) {
            case ClusterDataModule.KIND_WHITELIST -> present ? delegate.addToWhitelist(player) : delegate.removeFromWhitelist(player);
            default -> present ? delegate.enablePlayerNotifications(player) : delegate.disablePlayerNotifications(player);
        };
        return announce(write, kind, player, present);
    }

    private static Promise<Void> announce(Promise<Void> write, String kind, String key, Object document) {
        Optional<ClusterDataModule> module = announcing();
        if (module.isEmpty()) return write;
        // Resolves after the provider (and Redis) write and the announcement to the other nodes.
        return write.thenApply(v -> {
            module.get().announce(kind, key, document);
            return v;
        });
    }
}
