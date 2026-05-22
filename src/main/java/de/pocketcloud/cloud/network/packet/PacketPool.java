package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.packet.impl.TestPacket;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PacketPool {

    private static PacketPool instance;
    
    private final Map<String, Supplier<CloudPacket>> packets = new HashMap<>();
    
    public PacketPool() {
        instance = this;
        init();
    }
    
    private void init() {
        register(TestPacket.class, TestPacket::new);
    }

    public void register(Class<? extends CloudPacket> packetClass, Supplier<CloudPacket> supplier) {
        String packetName = packetClass.getSimpleName();
        packets.put(packetName, supplier);
    }

    public CloudPacket get(String packetName) {
        Supplier<CloudPacket> supplier = packets.get(packetName);
        return supplier != null ? supplier.get() : null;
    }

    public Map<String, Supplier<CloudPacket>> getAll() {
        return new HashMap<>(packets);
    }

    public static PacketPool getInstance() {
        if (instance == null) instance = new PacketPool();
        return instance;
    }
}