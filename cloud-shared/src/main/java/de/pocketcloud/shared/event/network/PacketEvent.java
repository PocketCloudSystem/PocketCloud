package de.pocketcloud.shared.event.network;

import de.pocketcloud.api.event.Event;
import de.pocketcloud.api.network.packet.Packet;
import lombok.Getter;

public abstract class PacketEvent implements Event {

    protected final Packet packet;

    public PacketEvent(Packet packet) {
        this.packet = packet;
    }
}