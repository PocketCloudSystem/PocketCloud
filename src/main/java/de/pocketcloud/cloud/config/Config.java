package de.pocketcloud.cloud.config;

import de.pocketcloud.cloud.config.exception.UnsupportedFileExtensionException;
import de.pocketcloud.cloud.config.type.ConfigType;
import de.pocketcloud.cloud.config.type.ConfigTypes;
import de.pocketcloud.cloud.console.log.CloudLogger;
import lombok.Getter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class Config {

    private final Path filePath;
    private ConfigType type;
    private Map<String, Object> content = Map.of();
    private final Map<String, Object> defaultContent;

    @Getter
    private boolean changed = false;

    public Config(Path filePath, ConfigType type, Map<String, Object> defaultContent) throws IOException, UnsupportedFileExtensionException {
        this.filePath = filePath;
        this.type = type;
        this.defaultContent = defaultContent;

        load();
    }

    public Config(String filePath, ConfigType type, Map<String, Object> defaultContent) throws IOException, UnsupportedFileExtensionException {
        this(Path.of(filePath), type, defaultContent);
    }

    public Config(Path filePath, ConfigType type) throws IOException, UnsupportedFileExtensionException {
        this(filePath, type, Map.of());
    }

    public Config(String filePath, ConfigType type) throws IOException, UnsupportedFileExtensionException {
        this(Path.of(filePath), type, Map.of());
    }

    public Config(Path filePath) throws IOException, UnsupportedFileExtensionException {
        this(filePath, null, Map.of());
    }

    public Config(String filePath) throws IOException, UnsupportedFileExtensionException {
        this(Path.of(filePath), null, Map.of());
    }

    public void load() throws IOException, UnsupportedFileExtensionException {
        if (!Files.exists(filePath.getParent())) throw new FileNotFoundException("Parent of " + filePath + " not found");
        if (type == null) type = ConfigTypes.detect(filePath);

        if (type == null) throw new UnsupportedFileExtensionException("Unsupported file extension from " + filePath + " has no config type");
        File file = filePath.toFile();
        if (!file.exists()) {
            if (!file.createNewFile()) throw new FileNotFoundException("Could not create file " + file.getAbsolutePath());
            content = defaultContent;
            changed = true;
            if (!save()) throw new IOException("Could not save file " + file.getAbsolutePath());
        } else {
            String content = Files.readString(filePath);

            try {
                this.content = type.decode(content);
            } catch (Exception e) {
                throw new IOException("Error reading config file " + filePath.toAbsolutePath(), e);
            }
        }
    }

    public void reload() throws IOException, UnsupportedFileExtensionException {
        content = Map.of();
        changed = false;
        load();
    }

    public boolean save() {
        return save(null);
    }

    /**
     * @param customSaveHandler If applied, the config will use this to save the config to disk, meaning the handler will save the config.
     */
    public boolean save(Function<Config, Boolean> customSaveHandler) {
        if (!changed) return true;
        if (customSaveHandler != null) return customSaveHandler.apply(this);

        try {
            String content = type.encode(this.content);
            Files.writeString(filePath, content);
            return true;
        } catch (Exception e) {
            CloudLogger.get().exception("Error writing config file {}", e, filePath.toAbsolutePath().toString());
            return false;
        }
    }

    public Config set(String key, Object value) {
        String[] keys = key.split("\\.");
        Map<String, Object> current = content;

        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.get(keys[i]);
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<String, Object>();
                current.put(keys[i], next);
            }

            current = (Map<String, Object>) next;
        }

        current.put(keys[keys.length - 1], value);
        changed = true;
        return this;
    }

    public Config setAll(Map<String, Object> content) {
        this.content = content;
        changed = true;
        return this;
    }

    @SuppressWarnings("unchecked")
    public Config remove(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> current = content;

        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.get(keys[i]);
            if (!(next instanceof Map)) return this;
            current = (Map<String, Object>) next;
        }

        current.remove(keys[keys.length - 1]);
        changed = true;
        return this;
    }

    public Config clear() {
        this.content = Map.of();
        changed = true;
        return this;
    }

    public boolean has(String key) {
        String[] keys = key.split("\\.");
        Map<String, Object> current = content;

        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.get(keys[i]);
            if (!(next instanceof Map)) return false;
            current = (Map<String, Object>) next;
        }

        return current.containsKey(keys[keys.length - 1]);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type, T defaultValue) {
        String[] keys = key.split("\\.");
        Map<String, Object> current = content;

        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.get(keys[i]);
            if (!(next instanceof Map)) return defaultValue;
            current = (Map<String, Object>) next;
        }

        Object value = current.get(keys[keys.length - 1]);
        return type.isInstance(value) ? type.cast(value) : defaultValue;
    }

    public <T> T get(String key, T defaultValue) {
        return get(key, (Class<T>) defaultValue.getClass(), defaultValue);
    }

    public Map<String, Object> getAll() {
        return content;
    }

    public List<String> getKeys() {
        return content.keySet().stream().toList();
    }
}