package packet;

import packet.util.PacketData;

/**
 * Base interface for all packets in the network system.
 */
public interface Packet {

    void encode(PacketData packetData);

    void encodePayload(PacketData packetData);

    void decode(PacketData packetData);

    void decodePayload(PacketData packetData);

    void handle();

    String getName();

    boolean isEncoded();

    Long getSentTimestamp();
}
