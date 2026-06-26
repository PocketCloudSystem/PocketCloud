package de.pocketcloud.cloud.network.packet;

import de.pocketcloud.cloud.network.packet.data.PacketData;

/**
 * Marker interface for packets that are sent from the Client (Server) to the Cloud.
 * CloudboundPacket -> Cloud is the receiver, Server (Client) is the sender
 */
public interface CloudboundPacket extends Packet {

    @Override
    void encodePayload(PacketData packetData);

    @Override
    default void decodePayload(PacketData packetData) {}
}