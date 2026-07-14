package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import io.netty.channel.Channel;
import lombok.Getter;

@Getter
public class PacketSentEvent extends PacketEvent {

    private final Channel receiver;

    public PacketSentEvent(Channel receiver, ClientboundPacket packet) {
        super(packet);
        this.receiver = receiver;
    }
}
