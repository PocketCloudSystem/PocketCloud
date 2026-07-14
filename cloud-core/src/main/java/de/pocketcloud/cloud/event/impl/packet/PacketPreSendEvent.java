package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.api.network.packet.ClientboundPacket;
import de.pocketcloud.cloud.event.Cancelable;
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
