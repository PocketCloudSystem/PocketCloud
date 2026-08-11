package de.pocketcloud.api.provider;

import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.api.network.packet.Packet;

import java.util.Collection;
import java.util.function.BiConsumer;

public interface IPacketRegistry<T> {

    void registerPacket(Class<? extends Packet> packetClass);

    void registerPacketListener(PacketListener packetListener);

    <U extends Packet> void registerPacketHandler(Class<U> packet, BiConsumer<U, T> handler);

    void invokeHandlers(Packet packet, T sender);

    Packet get(String packetName);

    <U extends Packet> U get(String packetName, Class<U> expectedPacket);

    int packetCount();

    Collection<Class<? extends Packet>> getAll();
}