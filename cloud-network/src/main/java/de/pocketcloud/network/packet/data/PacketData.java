package de.pocketcloud.network.packet.data;

import com.google.gson.*;
import de.pocketcloud.common.util.FileUtils;
import de.pocketcloud.common.serialization.Writable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public Long readLong() {
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

    @SuppressWarnings("unchecked")
    public List<Object> readArray() {
        Object read = read();
        if (read instanceof List<?> list) return (List<Object>) list;
        throw new PacketDecodeException("Array", read);
    }

    public <T> List<T> readArray(Class<T> type) {
        List<Object> raw = readArray();
        List<T> result = new ArrayList<>(raw.size());
        for (Object item : raw) {
            result.add(coerce(item, type));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> readMap() {
        Object read = read();
        if (read instanceof Map<?, ?> map) return (Map<String, Object>) map;
        throw new PacketDecodeException("Map", read);
    }

    public <T> Map<String, T> readMap(Class<T> valueType) {
        Map<String, Object> raw = readMap();
        Map<String, T> result = new LinkedHashMap<>(raw.size());
        for (Map.Entry<String, Object> entry : raw.entrySet()) {
            result.put(entry.getKey(), coerce(entry.getValue(), valueType));
        }
        return result;
    }

    public <T extends Enum<T>> T readEnum(Class<T> type) {
        String s = readString();
        try {
            return Enum.valueOf(type, s);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(type.getSimpleName() + " '" + s + "' not found", e);
        }
    }

    public boolean isEmpty() {
        return readIndex >= data.size();
    }

    public int remaining() {
        return data.size() - readIndex;
    }

    public List<Object> data() {
        return new ArrayList<>(data);
    }

    public String toJson() {
        return FileUtils.encodeJson(data);
    }

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

    @SuppressWarnings("unchecked")
    private <T> T coerce(Object item, Class<T> type) {
        if (item == null) return null;
        if (type.isInstance(item)) return type.cast(item);

        if (Number.class.isAssignableFrom(type) && item instanceof Number number) {
            if (type == Integer.class) return (T) Integer.valueOf(number.intValue());
            if (type == Long.class) return (T) Long.valueOf(number.longValue());
            if (type == Double.class) return (T) Double.valueOf(number.doubleValue());
            if (type == Float.class) return (T) Float.valueOf(number.floatValue());
            if (type == Short.class) return (T) Short.valueOf(number.shortValue());
            if (type == Byte.class) return (T) Byte.valueOf(number.byteValue());
        }

        throw new PacketDecodeException(type.getSimpleName(), item);
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