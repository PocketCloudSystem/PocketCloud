package de.pocketcloud.cloud.server.util;

import de.pocketcloud.common.serialization.Writable;
import de.pocketcloud.common.mapper.MapperUtils;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ServerCrashData(String serverName, UUID serverUuid, String type, String error, List<String> stackTrace,
                              String message, String file, Long line) implements Writable<Map<String, Object>> {

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static ServerCrashData read(Map<String, Object> data) {
        return MapperUtils.fromMap(data, ServerCrashData.class);
    }
}