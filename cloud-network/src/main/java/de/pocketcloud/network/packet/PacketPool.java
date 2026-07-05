package de.pocketcloud.network.packet;

import de.pocketcloud.common.lifecycle.Loadable;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class PacketPool implements Loadable {

    @Getter
    private static PacketPool instance;

    private final Consumer<PacketPool> onLoad;
    private final Map<String, Supplier<Packet>> packets = new ConcurrentHashMap<>();

    public PacketPool(Consumer<PacketPool> onLoad) {
        instance = this;
        this.onLoad = onLoad;
    }

    @Override
    public void load() {
        this.onLoad.accept(this);
    }

    @Override
    public void unload() {
        packets.clear();
    }

    public void register(Class<? extends Packet> packetClass, Supplier<Packet> supplier) {
        synchronized (packets) {
            String packetName = packetClass.getSimpleName();
            packets.put(packetName, supplier);
        }
    }

    public Packet get(String packetName) {
        Supplier<Packet> supplier = packets.get(packetName);
        return supplier != null ? supplier.get() : null;
    }

    public Map<String, Supplier<Packet>> getAll() {
        return new HashMap<>(packets);
    }
}