package packet;

import packet.impl.TestPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class PacketPool {
    private static PacketPool instance;
    
    private final Map<String, Supplier<CloudPacket>> packets = new HashMap<>();
    
    private PacketPool() {
        init();
    }
    
    public static void initialize() {
        instance = new PacketPool();
    }
    
    public static PacketPool getInstance() {
        if (instance == null) {
            initialize();
        }
        return instance;
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
}