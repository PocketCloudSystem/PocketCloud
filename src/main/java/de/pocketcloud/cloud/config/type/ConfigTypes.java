package de.pocketcloud.cloud.config.type;

import de.pocketcloud.cloud.util.FileUtils;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Getter
@Accessors(fluent = true)
public enum ConfigTypes {

    YAML(new YamlConfigType(), List.of("yaml", "yml")),
    JSON(new JsonConfigType(), List.of("json")),
    PROPERTIES(new PropertiesConfigType(), List.of("properties")),
    ENV(new EnvironmentConfigType(), List.of("env"));

    private final ConfigType type;
    private final List<String> extensions;

    ConfigTypes(ConfigType type, List<String> extensions) {
        this.type = type;
        this.extensions = extensions;
    }

    public static ConfigType detect(Path path) {
        if (Files.isRegularFile(path)) {
            String extension = FileUtils.extensionOf(path);
            for (ConfigTypes configType : ConfigTypes.values()) {
                if (configType.extensions().contains(extension)) {
                    return configType.type();
                }
            }
        }

        return null;
    }
}