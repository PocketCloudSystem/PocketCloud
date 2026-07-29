package de.pocketcloud.shared.event.network;

import de.pocketcloud.api.network.packet.Packet;
import io.netty.channel.Channel;
import lombok.Getter;

@Getter
public final class PacketSentEvent extends PacketEvent {

    private final Channel receiver;

    public PacketSentEvent(Packet packet, Channel receiver) {
        super(packet);
        this.receiver = receiver;
    }

    public Packet getPacket() {
        return packet;
    }
}