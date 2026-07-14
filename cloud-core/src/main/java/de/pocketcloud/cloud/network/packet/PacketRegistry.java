package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.network.packet.Packet;
import de.pocketcloud.network.packet.handler.PacketHandler;
import de.pocketcloud.network.packet.handler.PacketListener;
import de.pocketcloud.api.provider.IPacketRegistry;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class PacketRegistry implements IPacketRegistry, Loadable {

    private final Map<String, Class<? extends Packet>> packets = new ConcurrentHashMap<>();
    private final Map<Class<? extends Packet>, List<Consumer<Packet>>> handlers = new ConcurrentHashMap<>();

    @Override
    public void load() {
        Reflections reflections = new Reflections("de.pocketcloud.network.packet.def");
        Set<Class<? extends Packet>> packetClasses = reflections.getSubTypesOf(Packet.class);
        for (Class<? extends Packet> packetClass : packetClasses) {
            registerPacket(packetClass);
        }
    }

    @Override
    public void unload() {
        packets.clear();
    }

    @Override
    public void registerPacket(Class<? extends Packet> packetClass) {
        synchronized (packets) {
            packets.put(packetClass.getSimpleName(), packetClass);
        }
    }

    @Override
    public void registerPacketListener(PacketListener packetListener) {
        for (Method method : packetListener.getClass().getDeclaredMethods()) {
            method.setAccessible(true);
            if (method.isAnnotationPresent(PacketHandler.class)) {
                Class<? extends Packet>[] packets =  method.getAnnotation(PacketHandler.class).value();
                for (Class<? extends Packet> packet : packets) {
                    this.registerPacketHandler(packet, p -> {
                        try {
                            method.invoke(packetListener, p);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Packet> void registerPacketHandler(Class<T> packet, Consumer<T> handler) {
        if (!handlers.containsKey(packet)) handlers.put(packet, Collections.synchronizedList(new ArrayList<>()));
        List<Consumer<Packet>> methods = handlers.get(packet);
        methods.add((Consumer<Packet>) handler);
    }

    @Override
    public void invokeHandlers(Packet packet) {
        for (Consumer<Packet> handler : handlers.get(packet.getClass())) {
            handler.accept(packet);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Packet> T get(String packetName, Class<T> expectedPacket) {
        Packet packet = get(packetName);
        if (packet == null) return null;
        return (T) packet;
    }

    @Override
    public Packet get(String packetName) {
        Class<? extends Packet> packetClass = packets.getOrDefault(packetName, null);
        if (packetClass == null) return null;
        try {
            return packetClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException _) {
            return null;
        }
    }

    @Override
    public Collection<Class<? extends Packet>> getAll() {
        return packets.values();
    }
}