package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.event.impl.network.NetworkEvent;
import de.pocketcloud.cloud.network.NetworkNettyServer;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.network.packet.CloudboundPacket;
import lombok.Getter;

public final class PacketReceiveEvent extends NetworkEvent implements Cancelable {

    @Getter
    private final ServerClient sender;
    @Getter
    private final CloudboundPacket packet;

    public PacketReceiveEvent(NetworkNettyServer network, ServerClient sender, CloudboundPacket packet) {
        super(network);
        this.sender = sender;
        this.packet = packet;
    }
}
