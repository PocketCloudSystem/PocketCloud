package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.load.Loadable;
import de.pocketcloud.cloud.network.packet.impl.TestPacket;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class PacketPool implements Loadable {

    @Getter
    @Accessors(fluent = false)
    private static PacketPool instance;
    
    private final Map<String, Supplier<CloudPacket>> packets = new ConcurrentHashMap<>();
    
    public PacketPool() {
        instance = this;
    }
    
    public void load() {
        register(TestPacket.class, TestPacket::new);
    }

    @Override
    public void unload() {
        packets.clear();
    }

    public void register(Class<? extends CloudPacket> packetClass, Supplier<CloudPacket> supplier) {
        synchronized (packets) {
            String packetName = packetClass.getSimpleName();
            packets.put(packetName, supplier);
        }
    }

    public CloudPacket get(String packetName) {
        Supplier<CloudPacket> supplier = packets.get(packetName);
        return supplier != null ? supplier.get() : null;
    }

    public Map<String, Supplier<CloudPacket>> getAll() {
        return new HashMap<>(packets);
    }
}