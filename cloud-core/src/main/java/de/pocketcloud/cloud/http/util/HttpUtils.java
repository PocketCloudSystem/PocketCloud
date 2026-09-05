package de.pocketcloud.cloud.http.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.pocketcloud.common.serialization.Writable;
import de.pocketcloud.common.util.FileUtils;

import java.util.Map;

public final class HttpUtils {

    public static JsonElement toElement(Object object) {
        return FileUtils.GSON.toJsonTree(object);
    }

    public static void appendWritable(Writable<Map<String, Object>> writable, JsonObject jsonObject) {
        for (Map.Entry<String, Object> entry : writable.write().entrySet()) {
            jsonObject.add(entry.getKey(), toElement(entry.getValue()));
        }
    }

    public static Map<String, Object> toMap(JsonObject jsonObject) {
        return FileUtils.GSON.fromJson(jsonObject, new TypeToken<Map<String, Object>>() {}.getType());
    }
}