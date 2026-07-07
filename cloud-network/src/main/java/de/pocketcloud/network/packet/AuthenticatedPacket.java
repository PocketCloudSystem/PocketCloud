package de.pocketcloud.network.packet;

/**
 * If a packet is implementing this interface, it tells the cloud that the respective packet cannot be handled without a authenticated sender.
 * This means, that the server has to send the ServerHandshakeRequestPacket first in order to get verified.
 */
public interface AuthenticatedPacket {}