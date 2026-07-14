package de.pocketcloud.api.network.packet;

/**
 * Marker interface for packets that are sent from the Client (Server) to the Cloud.
 * CloudboundPacket -> Cloud is the receiver, Server (Client) is the sender
 */
public interface CloudboundPacket extends Packet {}