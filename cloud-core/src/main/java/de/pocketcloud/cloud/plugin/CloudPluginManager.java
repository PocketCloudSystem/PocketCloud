package de.pocketcloud.cloud.plugin;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.plugin.PluginDisableEvent;
import de.pocketcloud.cloud.event.impl.plugin.PluginEnableEvent;
import de.pocketcloud.cloud.event.impl.plugin.PluginLoadEvent;
import de.pocketcloud.cloud.plugin.exception.PluginLoadFailedException;
import de.pocketcloud.cloud.plugin.loader.JarCloudPluginLoader;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.common.lifecycle.Tickable;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Getter
public final class CloudPluginManager implements Tickable, Loadable {

    private final Map<String, CloudPlugin> plugins = new ConcurrentHashMap<>();
    private final JarCloudPluginLoader pluginLoader = new JarCloudPluginLoader();
    private final Path pluginsFolder = Paths.get("storage/plugins");

    public void load() {
        CloudLogger.get().info("Loading plugins...");
        List<CloudPlugin> eligiblePlugins = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsFolder)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    if (path.getFileName().toString().endsWith(".jar")) {
                        CloudPlugin pl = loadPlugin(path);
                        if (pl != null) eligiblePlugins.add(pl);
                    }
                }
            }
        } catch (IOException e) {
            CloudLogger.get().exception("Unable to load plugins", e);
        } finally {
            for (CloudPlugin pl : eligiblePlugins) {
                enable(pl);
            }
        }
    }

    @Override
    public void unload() {
        disableAll();
        this.plugins.clear();
    }

    public CloudPlugin loadPlugin(Path jarFile) {
        if (pluginLoader.canLoad(jarFile)) {
            CloudLogger.get().info("Loading plugin §b{}§r...", jarFile.getFileName().toString());
            CloudPlugin plugin;

            try {
                plugin = pluginLoader.load(jarFile);
            } catch (Exception e) {
                CloudLogger.get().exception("Unable to load plugin " + jarFile.getFileName(), e);
                return null;
            }

            try {
                if (plugins.containsKey(plugin.getDescription().name())) throw new PluginLoadFailedException("Plugin with the same name already loaded");
                plugins.put(plugin.getDescription().name(), plugin);
                new PluginLoadEvent(plugin).call();
                plugin.onLoad();
                return plugin;
            } catch (Exception e) {
                CloudLogger.get().exception("Unable to load plugin " + jarFile.getFileName(), e);
                disable(plugin);
            }
        }

        return null;
    }

    public void enableAll() {
        plugins.values().stream().filter(CloudPlugin::isDisabled).forEach(this::enable);
    }

    public void enable(CloudPlugin plugin) {
        if (plugin.isEnabled()) return;
        CloudLogger.get().info("§aEnabling §rplugin §b{}§r...", plugin.getDescription().name());
        plugin.setState(CloudPluginState.ENABLED);
        try {
            new PluginEnableEvent(plugin).call();
            plugin.onEnable();
        } catch (Exception e) {
            plugin.getLogger().exception("Unable to enable plugin", e);
            disable(plugin);
        }
    }

    public void disableAll() {
        CloudLogger.get().info("§cDisabling §rall plugins...");
        plugins.values().stream().filter(CloudPlugin::isEnabled).forEach(this::disable);
    }

    public void disable(CloudPlugin plugin) {
        if (plugin.isDisabled()) return;
        CloudLogger.get().info("§cDisabling §rplugin §b{}§r...", plugin.getDescription().name());
        plugins.remove(plugin.getDescription().name());
        plugin.setState(CloudPluginState.DISABLED);
        PocketCloud.instance().events().unregisterAll(plugin);
        try {
            new PluginDisableEvent(plugin).call();
            plugin.onDisable();
        } catch (Exception e) {
            plugin.getLogger().exception("Unable to disable plugin", e);
        } finally {
            try {
                plugin.getLoader().close();
            } catch (Exception _) {}
        }
    }

    @Override
    public void tick(long currentTick) {
        for (CloudPlugin plugin : plugins.values()) {
            if (!plugin.isEnabled()) continue;
            plugin.getScheduler().tick(currentTick);
        }
    }

    public int pluginCount() {
        return plugins.size();
    }

    public Optional<CloudPlugin> get(String name) {
        return Optional.ofNullable(plugins.get(name));
    }

    public List<CloudPlugin> getEnabledPlugins() {
        return plugins.values().stream().filter(CloudPlugin::isEnabled).collect(Collectors.toList());
    }

    public List<CloudPlugin> getDisabledPlugins() {
        return plugins.values().stream().filter(CloudPlugin::isDisabled).collect(Collectors.toList());
    }
}