package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.cloud.event.impl.network.NetworkEvent;
import lombok.Getter;

public abstract class PacketEvent extends NetworkEvent {

    @Getter
    private final Packet packet;

    public PacketEvent(Packet packet) {
        this.packet = packet;
    }
}