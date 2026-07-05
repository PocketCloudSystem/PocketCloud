package packet.util;

import com.google.gson.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PacketData {

    private final List<Object> data;
    private int readIndex = 0;
    private static final Gson GSON = new Gson();

    public PacketData() {
        this.data = new ArrayList<>();
    }

    public PacketData(List<Object> data) {
        this.data = new ArrayList<>(data);
    }

    public PacketData write(Object value) {
        data.add(value);
        return this;
    }

    public void writeAll(Object... values) {
        for (Object item : values) {
            if (item instanceof Writable writable) {
                write(writable.write());
            } else {
                write(item);
            }
        }
    }

    public Object read() {
        if (readIndex >= data.size()) return null;
        return data.get(readIndex++);
    }

    public String readString() {
        Object read = read();
        if (read == null) return null;
        return String.valueOf(read);
    }

    public Integer readInt() {
        Object read = read();
        if (read == null) return null;
        if (read instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(read));
    }

    public Long readLong() {
        Object read = read();
        if (read == null) return null;
        if (read instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(read));
    }

    public Float readFloat() {
        Object read = read();
        if (read == null) return null;
        if (read instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(String.valueOf(read));
    }

    public Double readDouble() {
        Object read = read();
        if (read == null) return null;
        if (read instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(String.valueOf(read));
    }

    public Boolean readBool() {
        Object read = read();
        if (read == null) return null;
        if (read instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(read));
    }

    @SuppressWarnings("unchecked")
    public List<Object> readArray() {
        Object read = read();
        if (read instanceof List<?> list) {
            List<Object> newList = new ArrayList<>();
            for (var item : list) {
                if (item instanceof JsonElement element) newList.add(toObject(element));
                else newList.add(item);
            }

            return newList;
        } else if (read instanceof JsonArray jsonArray) {
            return (List<Object>) toObject(jsonArray);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> readMap() {
        Object read = read();
        if (read instanceof Map) {
            return (Map<String, Object>) read;
        } else if (read instanceof JsonObject jsonObject) {
            return (Map<String, Object>) toObject(jsonObject);
        }

        return null;
    }

    public boolean isEmpty() {
        return readIndex >= data.size();
    }

    public int count() {
        return data.size() - readIndex;
    }

    public List<Object> getData() {
        return new ArrayList<>(data);
    }

    public String toJson() {
        return GSON.toJson(data);
    }

    public static PacketData fromJson(String json) {
        JsonArray jsonArray = GSON.fromJson(json, JsonArray.class);
        List<Object> list = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            if (element.isJsonPrimitive()) {
                var primitive = element.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    list.add(primitive.getAsBoolean());
                } else if (primitive.isNumber()) {
                    list.add(primitive.getAsNumber());
                } else {
                    list.add(primitive.getAsString());
                }
            } else if (element.isJsonArray()) {
                List<Object> subList = new ArrayList<>();
                for (JsonElement subElement : element.getAsJsonArray()) {
                    subList.add(subElement);
                }
                list.add(subList);
            } else if (element.isJsonObject()) {
                list.add(element.getAsJsonObject());
            } else {
                list.add(element);
            }
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

        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<String, JsonElement> e : el.getAsJsonObject().entrySet()) {
            map.put(e.getKey(), toObject(e.getValue()));
        }

        return map;
    }

    public interface Writable {

        Object write();
    }
}