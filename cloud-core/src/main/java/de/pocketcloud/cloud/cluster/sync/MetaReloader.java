package de.pocketcloud.cloud.cluster.sync;

import de.pocketcloud.api.component.group.IServerGroup;
import de.pocketcloud.api.component.template.ITemplate;
import de.pocketcloud.api.search.ServerSearchQuery;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.provider.CloudJsonProvider;
import de.pocketcloud.cloud.provider.CloudProvider;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.TemplateManager;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.template.group.ServerGroupManager;

import java.util.Objects;

/**
 * After the JSON provider files (templates.json, groups.json) were replicated: re-reads them and brings
 * the in-memory templates and groups up to date, so e.g. {@code server start} works for a template
 * that was created on another node.
 */
final class MetaReloader {

    private MetaReloader() {
    }

    static void reload() {
        if (!(CloudProvider.current() instanceof CloudJsonProvider json)) return;
        if (!PocketCloud.instance().serverGroups().isLoaded()) return;
        try {
            json.reloadTemplatesAndGroups();
        } catch (Exception e) {
            CloudLogger.get().exception("Failed to reload the replicated templates.json/groups.json", e);
            return;
        }

        TemplateManager templates = PocketCloud.instance().templates();
        json.getTemplates().thenSuccess(loaded -> {
            for (Template fresh : loaded.values()) {
                ITemplate existing = templates.get(fresh.name()).orElse(null);
                if (existing == null) {
                    templates.add(fresh);
                    fresh.syncOut();
                    CloudLogger.get().info("Template §b{} §rwas created on another node and is now available here.", fresh.name());
                } else if (!Objects.equals(existing.write(), fresh.write())) {
                    existing.settings().applyFrom(fresh.settings());
                    if (existing instanceof Template t) t.syncOut();
                    CloudLogger.get().info("Settings of template §b{} §rwere updated from the cluster.", fresh.name());
                }
            }
            for (ITemplate existing : templates.getAll()) {
                if (loaded.containsKey(existing.name())) continue;
                if (!PocketCloud.instance().servers().query(ServerSearchQuery.create().ofTemplate(existing)).isEmpty()) continue;
                templates.remove(existing);
                if (existing instanceof Template t) t.markForRemoval().syncOut();
                CloudLogger.get().info("Template §b{} §rwas removed on another node.", existing.name());
            }
        });

        ServerGroupManager groups = PocketCloud.instance().serverGroups();
        json.getServerGroups().thenSuccess(loaded -> {
            for (ServerGroup fresh : loaded.values()) {
                IServerGroup existing = groups.get(fresh.name()).orElse(null);
                if (existing == null || !Objects.equals(existing.write(), fresh.write())) {
                    groups.add(fresh);
                    fresh.syncOut();
                }
            }
            for (IServerGroup existing : groups.getAll()) {
                if (!loaded.containsKey(existing.name())) groups.remove(existing);
            }
        });
    }
}
