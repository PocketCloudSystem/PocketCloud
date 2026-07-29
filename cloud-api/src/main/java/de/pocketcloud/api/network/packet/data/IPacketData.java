package de.pocketcloud.api.network.packet.data;

import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

public interface IPacketData {

    IPacketData write(Object value);

    void writeAll(Object... values);

    Object read();

    Object peek();

    Object readLast();

    Object peekLast();

    String readString();

    Integer readInt();

    Long readLong();

    Float readFloat();

    Double readDouble();

    Boolean readBool();

    List<Object> readArray();

    <T> List<T> readArray(Class<T> type);

    <T> List<T> readArray(TypeToken<T> type);

    Map<String, Object> readMap();

    <T> Map<String, T> readMap(Class<T> valueType);

    <T> Map<String, T> readMap(TypeToken<T> type);

    <T extends Enum<T>> T readEnum(Class<T> type);

    IPacketData copyRemaining();

    boolean isEmpty();

    int remaining();

    byte[] toByteArray();
}