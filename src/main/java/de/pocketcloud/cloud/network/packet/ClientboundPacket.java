package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.packet.util.PacketData;

/**
 * Marker interface for packets that are sent from the Cloud to the Client (Server).
 * ClientboundPacket -> Server (Client) is the receiver, Cloud is the sender
 */
public interface ClientboundPacket extends Packet {

    @Override
    void decodePayload(PacketData packetData);

    @Override
    default void encodePayload(PacketData packetData) {}
}