package de.pocketcloud.bridge.network.packet;

import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.provider.IPacketRegistry;
import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.network.packet.handler.NormalPacketHandler;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.network.packet.broadcast.InternalPacketBroadcaster;
import io.netty.channel.Channel;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class PacketRegistry implements IPacketRegistry<Channel>, Loadable {

    private final Map<String, Class<? extends Packet>> packets = new ConcurrentHashMap<>();
    private final Map<Class<? extends Packet>, List<BiConsumer<Packet, Channel>>> handlers = new ConcurrentHashMap<>();

    @Override
    public void preload() {
        registerPacketListener(new NormalPacketHandler());

        InternalPacketBroadcaster.setBroadcasterHandler((pk, _) -> {
            if (pk instanceof CloudboundPacket p) {
                CloudBridge.instance().network().sendPacket(p).exceptionally(e -> {
                    CloudBridge.instance().logger().exception("Failed to send packet " + pk.getName(), e);
                    return null;
                });
            }
        });
    }

    @Override
    public void load() {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackage("de.pocketcloud.network.packet.impl", CloudBridge.class.getClassLoader())
                .addClassLoaders(CloudBridge.class.getClassLoader()));

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
                Class<? extends Packet>[] packets = method.getAnnotation(PacketHandler.class).value();
                for (Class<? extends Packet> packet : packets) {
                    this.registerPacketHandler(packet, (p, c) -> {
                        try {
                            switch (method.getParameterCount()) {
                                case 0: {
                                    method.invoke(packetListener);
                                    break;
                                }
                                case 1: {
                                    method.invoke(packetListener, p);
                                    break;
                                }
                                default: {
                                    method.invoke(packetListener, p, c);
                                    break;
                                }
                            }
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends Packet> void registerPacketHandler(Class<U> packet, BiConsumer<U, Channel> handler) {
        if (!handlers.containsKey(packet)) handlers.put(packet, Collections.synchronizedList(new ArrayList<>()));
        List<BiConsumer<Packet, Channel>> methods = handlers.get(packet);
        methods.add((BiConsumer<Packet, Channel>) handler);
    }

    @Override
    public void invokeHandlers(Packet packet, Channel sender) {
        for (BiConsumer<Packet, Channel> handler : handlers.getOrDefault(packet.getClass(), new ArrayList<>())) {
            handler.accept(packet, sender);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Packet> T get(String packetName, Class<T> expectedPacket) {
        Packet packet = get(packetName);
        if (packet == null) return null;
        return (T) packet;
    }

    @Override
    public Packet get(String packetName) {
        Class<? extends Packet> packetClass = packets.get(packetName);
        if (packetClass == null) return null;
        try {
            return packetClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException _) {
            return null;
        }
    }

    @Override
    public int packetCount() {
        return packets.size();
    }

    @Override
    public Collection<Class<? extends Packet>> getAll() {
        return List.copyOf(packets.values());
    }
}