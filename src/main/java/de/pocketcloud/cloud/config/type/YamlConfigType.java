package de.pocketcloud.cloud.config.type;

import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public final class YamlConfigType implements ConfigType {

    @Override
    public Map<String, Object> decode(String content) {
        return new Yaml().loadAs(content, Map.class);
    }

    @Override
    public String encode(Map<String, Object> content) {
        return new Yaml().dump(content);
    }
}