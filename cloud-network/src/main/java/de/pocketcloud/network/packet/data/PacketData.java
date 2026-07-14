package de.pocketcloud.network.packet.data;

import de.pocketcloud.api.network.packet.data.IPacketData;
import de.pocketcloud.common.serialization.Writable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PacketData implements IPacketData {

    private static final byte TYPE_NULL = 0;
    private static final byte TYPE_STRING = 1;
    private static final byte TYPE_INT = 2;
    private static final byte TYPE_LONG = 3;
    private static final byte TYPE_FLOAT = 4;
    private static final byte TYPE_DOUBLE = 5;
    private static final byte TYPE_BOOLEAN = 6;
    private static final byte TYPE_ARRAY = 7;
    private static final byte TYPE_MAP = 8;

    private final ByteBuf buffer;
    private int elementCount;

    public PacketData() {
        this.buffer = Unpooled.buffer();
        this.elementCount = 0;
    }

    public PacketData(ByteBuf buffer) {
        this.buffer = buffer;
        this.elementCount = countRemainingElements(buffer);
    }

    public PacketData write(Object value) {
        Object toWrite = value instanceof Writable<?> writable ? writable.write() : value;
        writeValue(buffer, toWrite);
        elementCount++;
        return this;
    }

    public void writeAll(Object... values) {
        for (Object item : values) write(item);
    }

    private static void writeValue(ByteBuf buf, Object value) {
        if (value == null) {
            buf.writeByte(TYPE_NULL);
        } else if (value instanceof String s) {
            buf.writeByte(TYPE_STRING);
            writeString(buf, s);
        } else if (value instanceof Integer i) {
            buf.writeByte(TYPE_INT);
            buf.writeInt(i);
        } else if (value instanceof Long l) {
            buf.writeByte(TYPE_LONG);
            buf.writeLong(l);
        } else if (value instanceof Float f) {
            buf.writeByte(TYPE_FLOAT);
            buf.writeFloat(f);
        } else if (value instanceof Double d) {
            buf.writeByte(TYPE_DOUBLE);
            buf.writeDouble(d);
        } else if (value instanceof Boolean b) {
            buf.writeByte(TYPE_BOOLEAN);
            buf.writeBoolean(b);
        } else if (value instanceof Enum<?> e) {
            buf.writeByte(TYPE_STRING);
            writeString(buf, e.name());
        } else if (value instanceof Number n) {
            buf.writeByte(TYPE_DOUBLE);
            buf.writeDouble(n.doubleValue());
        } else if (value instanceof List<?> list) {
            buf.writeByte(TYPE_ARRAY);
            buf.writeInt(list.size());
            for (Object item : list) {
                Object v = item instanceof Writable<?> writable ? writable.write() : item;
                writeValue(buf, v);
            }
        } else if (value instanceof Map<?, ?> map) {
            buf.writeByte(TYPE_MAP);
            buf.writeInt(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                writeString(buf, String.valueOf(entry.getKey()));
                Object v = entry.getValue() instanceof Writable<?> writable ? writable.write() : entry.getValue();
                writeValue(buf, v);
            }
        } else {
            buf.writeByte(TYPE_STRING);
            writeString(buf, value.toString());
        }
    }

    private static void writeString(ByteBuf buf, String value) {
        byte[] bytes = value.getBytes(CharsetUtil.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeBytes(bytes);
    }

    public Object read() {
        if (!buffer.isReadable()) throw new IndexOutOfBoundsException("Buffer is empty");
        Object value = readValue(buffer);
        if (elementCount > 0) elementCount--;
        return value;
    }

    public Object peek() {
        if (!buffer.isReadable()) throw new IndexOutOfBoundsException("Buffer is empty");
        buffer.markReaderIndex();
        Object value = readValue(buffer);
        buffer.resetReaderIndex();
        return value;
    }

    private static Object readValue(ByteBuf buf) {
        byte type = buf.readByte();
        return switch (type) {
            case TYPE_NULL -> null;
            case TYPE_STRING -> readStringValue(buf);
            case TYPE_INT -> buf.readInt();
            case TYPE_LONG -> buf.readLong();
            case TYPE_FLOAT -> buf.readFloat();
            case TYPE_DOUBLE -> buf.readDouble();
            case TYPE_BOOLEAN -> buf.readBoolean();
            case TYPE_ARRAY -> {
                int size = buf.readInt();
                List<Object> list = new ArrayList<>(size);
                for (int i = 0; i < size; i++) list.add(readValue(buf));
                yield list;
            }
            case TYPE_MAP -> {
                int size = buf.readInt();
                Map<String, Object> map = new LinkedHashMap<>(size);
                for (int i = 0; i < size; i++) {
                    String key = readStringValue(buf);
                    map.put(key, readValue(buf));
                }
                yield map;
            }
            default -> throw new PacketDecodeException("Unknown type tag", (int) type);
        };
    }

    private static String readStringValue(ByteBuf buf) {
        int length = buf.readInt();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, CharsetUtil.UTF_8);
    }

    public String readString() {
        return String.valueOf(read());
    }

    public Integer readInt() {
        Object read = read();
        if (read instanceof Number number) return number.intValue();
        return Integer.parseInt(String.valueOf(read));
    }

    public Long readLong() {
        Object read = read();
        if (read instanceof Number number) return number.longValue();
        return Long.parseLong(String.valueOf(read));
    }

    public Float readFloat() {
        Object read = read();
        if (read instanceof Number number) return number.floatValue();
        return Float.parseFloat(String.valueOf(read));
    }

    public Double readDouble() {
        Object read = read();
        if (read instanceof Number number) return number.doubleValue();
        return Double.parseDouble(String.valueOf(read));
    }

    public Boolean readBool() {
        Object read = read();
        if (read instanceof Boolean bool) return bool;
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
        return !buffer.isReadable();
    }

    public int remaining() {
        return elementCount;
    }

    public ByteBuf buffer() {
        return buffer;
    }
    public byte[] toByteArray() {
        ByteBuf dup = buffer.duplicate();
        byte[] bytes = new byte[dup.readableBytes()];
        dup.getBytes(dup.readerIndex(), bytes);
        return bytes;
    }

    public static PacketData fromByteBuf(ByteBuf buf) {
        return new PacketData(buf);
    }

    public static PacketData fromBytes(byte[] bytes) {
        return new PacketData(Unpooled.wrappedBuffer(bytes));
    }

    private static int countRemainingElements(ByteBuf buf) {
        ByteBuf dup = buf.duplicate();
        int count = 0;
        while (dup.isReadable()) {
            skipValue(dup);
            count++;
        }
        return count;
    }

    private static void skipValue(ByteBuf buf) {
        byte type = buf.readByte();
        switch (type) {
            case TYPE_NULL, TYPE_BOOLEAN -> {
                if (type == TYPE_BOOLEAN) buf.skipBytes(1);
            }
            case TYPE_INT -> buf.skipBytes(4);
            case TYPE_LONG -> buf.skipBytes(8);
            case TYPE_FLOAT -> buf.skipBytes(4);
            case TYPE_DOUBLE -> buf.skipBytes(8);
            case TYPE_STRING -> {
                int len = buf.readInt();
                buf.skipBytes(len);
            }
            case TYPE_ARRAY -> {
                int size = buf.readInt();
                for (int i = 0; i < size; i++) skipValue(buf);
            }
            case TYPE_MAP -> {
                int size = buf.readInt();
                for (int i = 0; i < size; i++) {
                    int keyLen = buf.readInt();
                    buf.skipBytes(keyLen);
                    skipValue(buf);
                }
            }
            default -> throw new PacketDecodeException("Unknown type tag", (int) type);
        }
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