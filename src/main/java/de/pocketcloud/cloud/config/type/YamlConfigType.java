package de.pocketcloud.cloud.config.type;

import de.pocketcloud.cloud.util.FileUtils;

import java.util.Map;

public final class YamlConfigType implements ConfigType {

    @Override
    public Map<String, Object> decode(String content) {
        return FileUtils.parseYaml(content);
    }

    @Override
    public String encode(Map<String, Object> content) {
        return FileUtils.emitYaml(content);
    }
}