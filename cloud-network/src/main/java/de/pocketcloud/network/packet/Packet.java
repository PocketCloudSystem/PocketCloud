package de.pocketcloud.network.packet;

import de.pocketcloud.network.packet.data.PacketData;
import io.netty.channel.Channel;
import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all packets in the network system.
 */
public interface Packet {

    void encode(PacketData packetData);

    void encodePayload(PacketData packetData);

    void decode(PacketData packetData);

    void decodePayload(PacketData packetData);

    void handle(@NotNull Channel channel);

    /**
     * This method is called after the serialization and the unserialization of a packet.
     */
    void setSize(long size);

    String getName();

    boolean isEncoded();

    Long getSentTimestamp();

    long getSize();
}