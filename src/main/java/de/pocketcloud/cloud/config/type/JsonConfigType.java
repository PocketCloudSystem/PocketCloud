package de.pocketcloud.cloud.config.type;

import de.pocketcloud.cloud.util.FileUtils;

import java.util.Map;

public final class JsonConfigType implements ConfigType {

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> decode(String content) {
        return FileUtils.decodeJson(content, Map.class);
    }

    @Override
    public String encode(Map<String, Object> content) {
        return FileUtils.encodeJson(content);
    }
}