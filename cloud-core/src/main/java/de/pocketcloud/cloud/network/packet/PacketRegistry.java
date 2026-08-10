package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.api.network.handler.PacketHandler;
import de.pocketcloud.api.network.handler.PacketListener;
import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.provider.IPacketRegistry;
import de.pocketcloud.cloud.network.broadcaster.PacketBroadcaster;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.handler.NormalPacketHandler;
import de.pocketcloud.cloud.network.packet.handler.PlayerPacketHandler;
import de.pocketcloud.cloud.network.packet.handler.ServerPacketHandler;
import de.pocketcloud.cloud.network.packet.handler.SyncPacketHandler;
import de.pocketcloud.common.lifecycle.Loadable;
import de.pocketcloud.network.packet.broadcast.InternalPacketBroadcaster;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class PacketRegistry implements IPacketRegistry<ServerClient>, Loadable {

    private final Map<String, Class<? extends Packet>> packets = new ConcurrentHashMap<>();
    private final Map<Class<? extends Packet>, List<BiConsumer<Packet, ServerClient>>> handlers = new ConcurrentHashMap<>();

    @Override
    public void preload() {
        registerPacketListener(new NormalPacketHandler());
        registerPacketListener(new ServerPacketHandler());
        registerPacketListener(new PlayerPacketHandler());
        registerPacketListener(new SyncPacketHandler());

        InternalPacketBroadcaster.setBroadcasterHandler((pk, ex) -> {
            if (pk instanceof ClientboundPacket p) {
                PacketBroadcaster.broadcast(p, ex::applyTo);
            }
        });
    }

    @Override
    public void load() {
        Reflections reflections = new Reflections("de.pocketcloud.network.packet.impl");
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
    public <U extends Packet> void registerPacketHandler(Class<U> packet, BiConsumer<U, ServerClient> handler) {
        if (!handlers.containsKey(packet)) handlers.put(packet, Collections.synchronizedList(new ArrayList<>()));
        List<BiConsumer<Packet, ServerClient>> methods = handlers.get(packet);
        methods.add((BiConsumer<Packet, ServerClient>) handler);
    }

    @Override
    public void invokeHandlers(Packet packet, ServerClient sender) {
        for (BiConsumer<Packet, ServerClient> handler : handlers.getOrDefault(packet.getClass(), new ArrayList<>())) {
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
    public Collection<Class<? extends Packet>> getAll() {
        return packets.values().stream().toList();
    }
}