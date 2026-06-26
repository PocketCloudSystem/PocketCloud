package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.impl.network.NetworkEvent;
import de.pocketcloud.cloud.network.NetworkNettyServer;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import lombok.Getter;

public final class PacketTooLargeEvent extends NetworkEvent {

    @Getter
    private final ServerClient receiver;
    @Getter
    private final ClientboundPacket packet;
    @Getter
    private final int size;
    @Getter
    private final String buffer;

    public PacketTooLargeEvent(NetworkNettyServer network, ServerClient receiver, ClientboundPacket packet, int size, String buffer) {
        super(network);
        this.receiver = receiver;
        this.packet = packet;
        this.size = size;
        this.buffer = buffer;
    }
}
