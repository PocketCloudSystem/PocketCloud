package de.pocketcloud.network.traffic;

import de.pocketcloud.api.network.packet.Packet;
import io.netty.channel.Channel;
import org.jetbrains.annotations.Nullable;

public interface PacketTrafficListener {

    Packet onPacketResolve(String packetName);

    boolean onOutgoing(Channel address, Packet packet, byte[] payload, int length);

    boolean onIncoming(Channel address, byte[] payload, int length);

    default void onUnknownPacket(Channel address, byte[] payload, int length) {}

    default void onTooLargePacket(Channel address, @Nullable Packet packet, int length, de.pocketcloud.api.network.traffic.TrafficDirection direction) {}

    PacketTrafficListener NOOP = new PacketTrafficListener() {

        @Override
        public Packet onPacketResolve(String packetName) {
            return null;
        }

        @Override
        public boolean onIncoming(Channel address, byte[] rawBytes, int length) {
            return true;
        }

        @Override
        public boolean onOutgoing(Channel address, Packet packet, byte[] payload, int length) {
            return true;
        }
    };
}