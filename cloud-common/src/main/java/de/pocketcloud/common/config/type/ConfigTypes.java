package de.pocketcloud.common.config.type;

import de.pocketcloud.common.util.FileUtils;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

    public static Optional<ConfigType> detect(Path path) {
        String extension = FileUtils.extensionOf(path);
        for (ConfigTypes configType : ConfigTypes.values()) {
            if (configType.extensions().contains(extension)) {
                return Optional.of(configType.type());
            }
        }

        return Optional.empty();
    }
}