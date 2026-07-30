package de.pocketcloud.cloud.plugin;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.log.CloudLogger;
import de.pocketcloud.cloud.console.log.def.PrefixedLogger;
import de.pocketcloud.cloud.scheduler.TaskScheduler;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Getter
public abstract class CloudPlugin {

    private boolean initialized = false;

    @Setter
    private CloudPluginState state = CloudPluginState.DISABLED;
    private CloudPluginClassLoader loader;
    private TaskScheduler scheduler;
    private PrefixedLogger logger;
    private CloudPluginDescription description;
    private Path dataFolder;
    private Path pluginFilePath;

    public CloudPlugin() {}

    public final void init(CloudPluginClassLoader loader, CloudPluginDescription description, Path dataFolder, Path pluginFilePath) {
        if (initialized) return;
        initialized = true;
        this.loader = loader;
        this.scheduler = new TaskScheduler(this);
        this.description = description;
        this.dataFolder = dataFolder;
        this.pluginFilePath = pluginFilePath;
        this.logger = CloudLogger.prefixed("[" + description.name() + "]");
    }

    public boolean saveResource(String relativePath, boolean overwrite) throws IOException {
        if (relativePath == null || relativePath.isBlank()) throw new IllegalArgumentException("relativePath cannot be null or blank");
        if (relativePath.contains("..")) throw new IllegalArgumentException("Path traversal is not allowed");
        Path destination = dataFolder.resolve(relativePath);
        if (Files.exists(destination) && !overwrite) return false;
        Files.createDirectories(destination.getParent());
        try (InputStream entryInputStream = getResource(relativePath)) {
            if (entryInputStream == null) return false;
            if (overwrite) {
                Files.copy(entryInputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(entryInputStream, destination);
            }
        }
        return true;
    }

    public void onLoad() {}

    public void onEnable() {}

    public void onDisable() {}

    public boolean isEnabled() {
        return state == CloudPluginState.ENABLED;
    }

    public boolean isDisabled() {
        return state == CloudPluginState.DISABLED;
    }

    @Nullable
    public InputStream getResource(String path) throws IOException {
        try (JarFile jarFile = new JarFile(pluginFilePath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(path);
            if (entry == null) return null;
            try (InputStream stream = jarFile.getInputStream(entry)) {
                return new ByteArrayInputStream(stream.readAllBytes());
            }
        }
    }

    final public PocketCloud getCloud() {
        return PocketCloud.instance();
    }
}