package de.pocketcloud.cloud.traffic.impl;

import de.pocketcloud.cloud.network.packet.CloudPacket;
import de.pocketcloud.cloud.network.packet.Packet;
import de.pocketcloud.cloud.traffic.TrafficMonitor;
import de.pocketcloud.cloud.traffic.TrafficMonitorManager;
import de.pocketcloud.cloud.util.TriConsumer;

import java.net.SocketAddress;

public final class NetworkTrafficMonitor extends TrafficMonitor {

    public static final String NETWORK_MODE_PACKET_IN = "packet_in";
    public static final String NETWORK_MODE_PACKET_OUT = "packet_out";

    public NetworkTrafficMonitor() {
        super(TrafficMonitorManager.TRAFFIC_NETWORK);
    }

    public <T extends CloudPacket> NetworkTrafficMonitor monitorPacketIn(Class<T> packetClass, TriConsumer<SocketAddress, T, Long> handler) {
        addHandler(parsePacketMode(NETWORK_MODE_PACKET_IN, packetClass), (a, b, by) -> {
            @SuppressWarnings("unchecked")
            T buffer = (T) b;
            handler.accept(a, buffer, by);
        });
        return this;
    }

    public <T extends CloudPacket> NetworkTrafficMonitor monitorPacketOut(Class<T> packetClass, TriConsumer<SocketAddress, T, Long> handler) {
        addHandler(parsePacketMode(NETWORK_MODE_PACKET_OUT, packetClass), (a, b, by) -> {
            @SuppressWarnings("unchecked")
            T buffer = (T) b;
            handler.accept(a, buffer, by);
        });
        return this;
    }

    public static String parsePacketMode(String normalMode, Class<? extends Packet> packetClass) {
        return normalMode + "-" + packetClass.getSimpleName();
    }
}