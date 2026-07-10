package de.pocketcloud.network.packet.type;

import de.pocketcloud.common.serialization.Writable;
import de.pocketcloud.common.mapper.MapperUtils;

import java.util.ArrayList;
import java.util.Map;

public record ServerCommandExecutionResult(String id, String commandLine, ArrayList<String> messages) implements Writable<Map<String, Object>> {

    public String message(int index) {
        try {
            return messages.get(index);
        } catch (IndexOutOfBoundsException _) {
            return null;
        }
    }

    @Override
    public Map<String, Object> write() {
        return MapperUtils.toMap(this);
    }

    public static ServerCommandExecutionResult read(Map<String, Object> data) {
        return MapperUtils.fromMap(data, ServerCommandExecutionResult.class);
    }
}