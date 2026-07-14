package de.pocketcloud.network.traffic.impl;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.common.function.TriConsumer;
import de.pocketcloud.network.traffic.TrafficDirection;
import de.pocketcloud.network.traffic.TrafficMonitor;
import io.netty.channel.Channel;

public final class NetworkTrafficMonitor extends TrafficMonitor {

    public <T extends Packet> NetworkTrafficMonitor monitorPacketIn(Class<T> packetClass, TriConsumer<Channel, T, Long> handler) {
        addHandler(parsePacketMode(TrafficDirection.IN, packetClass), (a, b, by) -> {
            @SuppressWarnings("unchecked")
            T buffer = (T) b;
            handler.accept(a, buffer, by);
        });
        return this;
    }

    public <T extends Packet> NetworkTrafficMonitor monitorPacketOut(Class<T> packetClass, TriConsumer<Channel, T, Long> handler) {
        addHandler(parsePacketMode(TrafficDirection.OUT, packetClass), (a, b, by) -> {
            @SuppressWarnings("unchecked")
            T buffer = (T) b;
            handler.accept(a, buffer, by);
        });
        return this;
    }

    public static TrafficDirection parsePacketMode(TrafficDirection regular, Class<? extends Packet> packetClass) {
        return new TrafficDirection("packet_" + regular.name().toLowerCase() + "_" + packetClass.getSimpleName());
    }
}