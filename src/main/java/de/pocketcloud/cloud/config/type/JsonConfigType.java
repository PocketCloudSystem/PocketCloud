package de.pocketcloud.cloud.config.type;

import com.google.gson.Gson;

import java.util.Map;

public final class JsonConfigType implements ConfigType {

    @Override
    public Map<String, Object> decode(String content) {
        return new Gson().fromJson(content, Map.class);
    }

    @Override
    public String encode(Map<String, Object> content) {
        return new Gson().toJson(content);
    }
}