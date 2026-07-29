package de.pocketcloud.shared.event.network;

import de.pocketcloud.api.network.packet.Packet;
import io.netty.channel.Channel;
import lombok.Getter;

@Getter
public final class PacketReceivedEvent extends PacketEvent {

    private final Channel sender;

    public PacketReceivedEvent(Packet packet, Channel sender) {
        super(packet);
        this.sender = sender;
    }

    public Packet getPacket() {
        return packet;
    }
}