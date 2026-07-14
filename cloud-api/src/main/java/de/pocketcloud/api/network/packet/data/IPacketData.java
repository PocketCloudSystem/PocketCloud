package de.pocketcloud.api.network.packet.data;

import java.util.List;
import java.util.Map;

public interface IPacketData {

    IPacketData write(Object value);

    void writeAll(Object... values);

    Object read();

    Object peek();

    String readString();

    Integer readInt();

    Long readLong();

    Float readFloat();

    Double readDouble();

    Boolean readBool();

    List<Object> readArray();

    <T> List<T> readArray(Class<T> type);

    Map<String, Object> readMap();

    <T> Map<String, T> readMap(Class<T> valueType);

    <T extends Enum<T>> T readEnum(Class<T> type);

    boolean isEmpty();

    int remaining();

    byte[] toByteArray();
}