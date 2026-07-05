package de.pocketcloud.cloud.server.util;

import de.pocketcloud.cloud.util.Writable;
import de.pocketcloud.cloud.util.mapper.MapperUtils;

import java.util.List;
import java.util.Map;

public record ServerCrashData(String type, String error, List<String> stackTrace,
                              String message, String file, Long line) implements Writable<Map<String, Object>> {

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static ServerCrashData read(Map<String, Object> data) {
        return MapperUtils.fromMap(data, ServerCrashData.class);
    }
}