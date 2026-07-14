package de.pocketcloud.api.network.packet;

import de.pocketcloud.api.network.packet.data.IPacketData;

/**
 * Base interface for all packets in the network system.
 */
public interface Packet {

    void encode(IPacketData packetData);

    void encodePayload(IPacketData packetData);

    void decode(IPacketData packetData);

    void decodePayload(IPacketData packetData);

    /**
     * This method is called after the serialization and the unserialization of a packet.
     */
    void setSize(long size);

    String getName();

    boolean isEncoded();

    Long getSentTimestamp();

    long getSize();
}