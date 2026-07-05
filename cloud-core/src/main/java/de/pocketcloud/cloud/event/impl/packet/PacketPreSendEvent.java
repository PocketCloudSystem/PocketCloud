package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.network.packet.ClientboundPacket;
import io.netty.channel.Channel;
import lombok.Getter;

@Getter
public class PacketPreSendEvent extends PacketEvent implements Cancelable {

    private final Channel receiver;

    public PacketPreSendEvent(Channel receiver, ClientboundPacket packet) {
        super(packet);
        this.receiver = receiver;
    }
}
