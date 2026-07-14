package de.pocketcloud.api.network.packet;

/**
 * Marker interface for packets that are sent from the Cloud to the Client (Server).
 * ClientboundPacket -> Server (Client) is the receiver, Cloud is the sender
 */
public interface ClientboundPacket extends Packet {}