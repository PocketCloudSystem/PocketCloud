package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.event.impl.network.NetworkEvent;
import de.pocketcloud.cloud.network.NetworkNettyServer;
import de.pocketcloud.cloud.network.client.ServerClient;
import de.pocketcloud.cloud.network.packet.ClientboundPacket;
import lombok.Getter;

public final class PacketPreSendEvent extends NetworkEvent implements Cancelable {

    @Getter
    private final ServerClient receiver;
    @Getter
    private final ClientboundPacket packet;

    public PacketPreSendEvent(NetworkNettyServer network, ServerClient receiver, ClientboundPacket packet) {
        super(network);
        this.receiver = receiver;
        this.packet = packet;
    }
}
