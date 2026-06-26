package de.pocketcloud.cloud.network.packet.data;

import com.google.gson.*;
import de.pocketcloud.cloud.network.packet.type.*;
import de.pocketcloud.cloud.player.CloudPlayer;
import de.pocketcloud.cloud.server.CloudServer;
import de.pocketcloud.cloud.server.util.ServerStatus;
import de.pocketcloud.cloud.template.Template;
import de.pocketcloud.cloud.template.group.ServerGroup;
import de.pocketcloud.cloud.util.FileUtils;
import de.pocketcloud.cloud.util.Writable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class PacketData {

    private final List<Object> data;
    private int readIndex = 0;

    public PacketData() {
        this.data = new ArrayList<>();
    }

    public PacketData(List<Object> data) {
        this.data = new ArrayList<>(data);
    }

    public PacketData write(Object value) {
        data.add(value instanceof Writable<?> writable ? writable.write() : value);
        return this;
    }

    public void writeAll(Object... values) {
        for (Object item : values) write(item);
    }

    public Object read() {
        if (readIndex >= data.size()) throw new IndexOutOfBoundsException("Buffer is empty");
        return data.get(readIndex++);
    }

    public Object peek() {
        if (readIndex >= data.size()) throw new IndexOutOfBoundsException("Buffer is empty");
        return data.get(readIndex);
    }

    public String readString() {
        return String.valueOf(read());
    }

    public Integer readInt() {
        Object read = read();
        if (read instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(read));
    }

    public @NotNull Long readLong() {
        Object read = read();
        if (read instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(read));
    }

    public Float readFloat() {
        Object read = read();
        if (read instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(String.valueOf(read));
    }

    public Double readDouble() {
        Object read = read();
        if (read instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(read));
    }

    public Boolean readBool() {
        Object read = read();
        if (read instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(read));
    }

//    @SuppressWarnings("unchecked")
//    public List<Object> readArray() {
//        Object read = read();
//        if (read instanceof List<?> list) {
//            List<Object> newList = new ArrayList<>();
//            for (var item : list) {
//                if (item instanceof JsonElement element) newList.add(toObject(element));
//                else newList.add(item);
//            }
//
//            return newList;
//        } else if (read instanceof JsonArray jsonArray) {
//            return (List<Object>) toObject(jsonArray);
//        }
//
//        return new ArrayList<>();
//    }

    @SuppressWarnings("unchecked")
    public List<Object> readArray() {
        Object read = read();
        if (read instanceof List<?> list) return (List<Object>) list;
        throw new PacketDecodeException("Array", read);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> readMap() {
        Object read = read();
        if (read instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new PacketDecodeException("Map", read);
    }

//    @SuppressWarnings("unchecked")
//    public Map<String, Object> readMap() {
//        Object read = read();
//        return switch (read) {
//            case null -> throw new IndexOutOfBoundsException("Buffer is empty");
//            case Map<?, ?> map -> (Map<String, Object>) read;
//            case JsonObject jsonObject -> (Map<String, Object>) toObject(jsonObject);
//            default -> new HashMap<>();
//        };
//    }

    public Template readTemplate() {
        return Template.read(readMap());
    }

    public CloudServer readServer() {
        return CloudServer.read(readMap());
    }

    public ServerGroup readServerGroup() {
        return ServerGroup.read(readMap());
    }

    public CloudPlayer readPlayer() {
        return CloudPlayer.read(readMap());
    }

    public ServerCommandExecutionResult readServerCommandExecutionResult() {
        return ServerCommandExecutionResult.read(readMap());
    }

    public <T extends Enum<T>> T readEnum(Class<T> type) {
        String s = readString();
        try {
            return Enum.valueOf(type, s);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(type.getSimpleName() + " '" + s + "' not found", e);
        }
    }

    public LogType readLogType() {
        return readEnum(LogType.class);
    }

    public NotificationType readNotificationType() {
        return readEnum(NotificationType.class);
    }

    public ServerStatus readServerStatus() {
        return readEnum(ServerStatus.class);
    }

    public ServerDisconnectReason readServerDisconnectReason() {
        return readEnum(ServerDisconnectReason.class);
    }

    public ActionFailureReason readActionFailureReason() {
        return readEnum(ActionFailureReason.class);
    }

    public VerificationStatus readVerifyStatus() {
        return readEnum(VerificationStatus.class);
    }

    public TextType readTextType() {
        return readEnum(TextType.class);
    }

    public boolean isEmpty() {
        return readIndex >= data.size();
    }

    public int remaining() {
        return data.size() - readIndex;
    }

    public List<Object> getData() {
        return new ArrayList<>(data);
    }

    public String toJson() {
        return FileUtils.encodeJson(data);
    }

//    public static PacketData fromJson(String json) {
//        JsonArray jsonArray = FileUtils.decodeJson(json, JsonArray.class);
//        List<Object> list = new ArrayList<>();
//        for (JsonElement element : jsonArray) {
//            if (element.isJsonPrimitive()) {
//                var primitive = element.getAsJsonPrimitive();
//                if (primitive.isBoolean()) {
//                    list.add(primitive.getAsBoolean());
//                } else if (primitive.isNumber()) {
//                    list.add(primitive.getAsNumber());
//                } else {
//                    list.add(primitive.getAsString());
//                }
//            } else if (element.isJsonArray()) {
//                List<Object> subList = new ArrayList<>();
//                for (JsonElement subElement : element.getAsJsonArray()) {
//                    subList.add(subElement);
//                }
//                list.add(subList);
//            } else if (element.isJsonObject()) {
//                list.add(element.getAsJsonObject());
//            } else {
//                list.add(element);
//            }
//        }
//        return new PacketData(list);
//    }

    public static PacketData fromJson(String json) {
        JsonArray jsonArray = FileUtils.decodeJson(json, JsonArray.class);
        List<Object> list = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            list.add(toObject(element));
        }
        return new PacketData(list);
    }

    public static Object toObject(JsonElement el) {
        if (el.isJsonNull()) return null;

        if (el.isJsonPrimitive()) {
            JsonPrimitive p = el.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) {
                String n = p.getAsString();
                return n.contains(".") ? Double.parseDouble(n) : Long.parseLong(n);
            }
            return p.getAsString();
        }

        if (el.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement e : el.getAsJsonArray()) {
                list.add(toObject(e));
            }
            return list;
        }

        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
            map.put(e.getKey(), toObject(e.getValue()));
        }

        return map;
    }

    public static final class PacketDecodeException extends RuntimeException {

        public PacketDecodeException(String enumTypeName, String value, Throwable cause) {
            super(enumTypeName + " '" + value + "' not found", cause);
        }

        public PacketDecodeException(String expectedType, Object actual) {
            super("Expected " + expectedType + " but got " + (actual == null ? "null" : actual.getClass().getSimpleName()));
        }
    }
}