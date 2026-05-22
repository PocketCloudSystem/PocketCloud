package de.pocketcloud.cloud.plugin;

import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.event.impl.plugin.PluginDisableEvent;
import de.pocketcloud.cloud.event.impl.plugin.PluginEnableEvent;
import de.pocketcloud.cloud.event.impl.plugin.PluginLoadEvent;
import de.pocketcloud.cloud.plugin.exception.PluginLoadFailedException;
import de.pocketcloud.cloud.plugin.loader.JarCloudPluginLoader;
import de.pocketcloud.cloud.tick.Tickable;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public final class CloudPluginManager implements Tickable {

    private final Map<String, CloudPlugin> plugins = new HashMap<>();
    private final JarCloudPluginLoader pluginLoader = new JarCloudPluginLoader();
    private final Path pluginsFolder = Paths.get("storage/plugins");

    public void loadAll() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(pluginsFolder)) {
            for (Path path : stream) {
                if (Files.isRegularFile(path)) {
                    if (path.getFileName().toString().endsWith(".jar")) {
                        loadPlugin(path);
                    }
                }
            }
        } catch (IOException e) {
            CloudLogger.get().exception("Unable to load plugins", e);
        }
    }

    public void loadPlugin(Path jarFile) {
        if (pluginLoader.canLoad(jarFile)) {
            try {
                CloudPlugin plugin = pluginLoader.load(jarFile);
                if (plugins.containsKey(plugin.getDescription().getName())) throw new PluginLoadFailedException("Plugin with the same name already loaded");
                plugins.put(plugin.getDescription().getName(), plugin);
                new PluginLoadEvent(plugin).call();
                plugin.onLoad();
            } catch (Exception e) {
                CloudLogger.get().exception("Unable to load plugin " + jarFile.getFileName(), e);
            }
        }
    }

    public void enableAll() {
        plugins.values().stream().filter(CloudPlugin::isDisabled).forEach(this::enable);
    }

    public void enable(CloudPlugin plugin) {
        if (plugin.isEnabled()) return;
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
        plugins.values().stream().filter(CloudPlugin::isEnabled).forEach(this::enable);
    }

    public void disable(CloudPlugin plugin) {
        if (plugin.isDisabled()) return;
        plugins.remove(plugin.getDescription().getName());
        plugin.setState(CloudPluginState.DISABLED);
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

    public void clear() {
        for (CloudPlugin plugin : plugins.values()) {
            try {
                plugin.getLoader().close();
            } catch (IOException _) {}
        }

        this.plugins.clear();
    }

    @Override
    public void tick(long currentTick) {
        //todo tick plugin schedulers
    }

    public CloudPlugin get(String name) {
        return plugins.getOrDefault(name, null);
    }

    public List<CloudPlugin> getEnabledPlugins() {
        return plugins.values().stream().filter(CloudPlugin::isEnabled).collect(Collectors.toList());
    }

    public List<CloudPlugin> getDisabledPlugins() {
        return plugins.values().stream().filter(CloudPlugin::isDisabled).collect(Collectors.toList());
    }
}