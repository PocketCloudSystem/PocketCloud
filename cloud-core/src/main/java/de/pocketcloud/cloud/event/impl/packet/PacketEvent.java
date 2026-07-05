package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.impl.network.NetworkEvent;
import de.pocketcloud.network.packet.Packet;
import lombok.Getter;

public abstract class PacketEvent extends NetworkEvent {

    @Getter
    private final Packet packet;

    public PacketEvent(Packet packet) {
        this.packet = packet;
    }
}