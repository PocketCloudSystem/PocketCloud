package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.packet.data.PacketData;
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

    String getName();

    boolean isEncoded();

    Long getSentTimestamp();
}