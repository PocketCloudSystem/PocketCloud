package de.pocketcloud.common.config.type;

import de.pocketcloud.common.util.FileUtils;

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