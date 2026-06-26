package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.data.PacketData;
import org.jetbrains.annotations.NotNull;

/**
 * Base interface for all packets in the network system.
 */
public interface Packet {

    void encode(PacketData packetData);

    void encodePayload(PacketData packetData);

    void decode(PacketData packetData);

    void decodePayload(PacketData packetData);

    void handle(@NotNull ServerClient client);

    String getName();

    boolean isEncoded();

    Long getSentTimestamp();
}