package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.impl.network.NetworkEvent;
import de.pocketcloud.cloud.network.NetworkNettyServer;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.ClientboundPacket;
import lombok.Getter;

public final class PacketSentEvent extends NetworkEvent {

    @Getter
    private final ServerClient receiver;
    @Getter
    private final ClientboundPacket packet;
    @Getter
    private final boolean success;

    public PacketSentEvent(NetworkNettyServer network, ServerClient receiver, ClientboundPacket packet, boolean success) {
        super(network);
        this.receiver = receiver;
        this.packet = packet;
        this.success = success;
    }
}
