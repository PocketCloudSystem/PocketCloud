package de.pocketcloud.cloud.config;

import de.pocketcloud.cloud.console.log.CloudLogger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Moves sections of an old config.yml into their own files (once, only if the file does not exist yet). */
public final class ConfigMigration {

    private ConfigMigration() {
    }

    /**
     * @param sections key in config.yml → file next to it
     * @return the files that were written (config.yml should then be saved without those keys)
     */
    public static List<String> splitSections(Path configFile, Map<String, String> sections) {
        if (!Files.isRegularFile(configFile)) return List.of();
        Map<?, ?> old;
        try {
            Object loaded = new Yaml().load(Files.readString(configFile));
            if (!(loaded instanceof Map<?, ?> map)) return List.of();
            old = map;
        } catch (Exception e) {
            CloudLogger.get().warn("Could not read §b{} §rto move its sections into their own files§8: §c{}", configFile, e.getMessage());
            return List.of();
        }
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        List<String> moved = new ArrayList<>();
        for (Map.Entry<String, String> section : sections.entrySet()) {
            Path target = configFile.resolveSibling(section.getValue());
            if (!(old.get(section.getKey()) instanceof Map<?, ?> values) || Files.exists(target)) continue;
            try {
                Files.writeString(target, new Yaml(options).dump(values));
                moved.add(section.getValue());
            } catch (IOException e) {
                CloudLogger.get().warn("Could not write §b{}§8: §c{}", target, e.getMessage());
            }
        }
        return moved;
    }
}
