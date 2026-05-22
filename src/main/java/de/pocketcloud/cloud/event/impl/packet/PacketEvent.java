package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.network.packet.CloudPacket;
import lombok.Getter;

public class PacketEvent {

    @Getter
    private final CloudPacket packet;

    public PacketEvent(CloudPacket packet) {
        this.packet = packet;
    }
}