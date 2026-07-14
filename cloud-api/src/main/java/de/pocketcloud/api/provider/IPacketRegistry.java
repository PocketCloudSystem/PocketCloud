package de.pocketcloud.api.provider;

import de.pocketcloud.network.packet.Packet;
import de.pocketcloud.network.packet.handler.PacketListener;

import java.util.Collection;
import java.util.function.Consumer;

public interface IPacketRegistry {

    void registerPacket(Class<? extends Packet> packetClass);

    void registerPacketListener(PacketListener packetListener);

    <T extends Packet> void registerPacketHandler(Class<T> packet, Consumer<T> handler);

    void invokeHandlers(Packet packet);

    Packet get(String packetName);

    <T extends Packet> T get(String packetName, Class<T> expectedPacket);

    Collection<Class<? extends Packet>> getAll();
}