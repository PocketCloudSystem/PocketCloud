package de.pocketcloud.network.traffic;

import de.pocketcloud.network.packet.Packet;
import io.netty.channel.Channel;
import org.jetbrains.annotations.Nullable;

public interface PacketTrafficListener {

    boolean onOutgoing(Channel address, Packet packet, byte[] rawBytes, int length, String payload);

    boolean onIncoming(Channel address, byte[] rawBytes, int length, String payload);

    default void onUnknownPacket(Channel address, byte[] rawBytes, int length, String payload) {}

    default void onTooLargePacket(Channel address, @Nullable Packet packet, int length, TrafficDirection direction) {}

    PacketTrafficListener NOOP = new PacketTrafficListener() {

        @Override
        public boolean onOutgoing(Channel address, Packet packet, byte[] rawBytes, int length, String payload) {
            return true;
        }

        @Override
        public boolean onIncoming(Channel address, byte[] rawBytes, int length, String payload) {
            return true;
        }
    };
}