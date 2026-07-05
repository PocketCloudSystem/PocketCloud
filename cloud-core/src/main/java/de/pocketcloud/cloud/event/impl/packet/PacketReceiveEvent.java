package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.network.packet.CloudboundPacket;
import io.netty.channel.Channel;
import lombok.Getter;

@Getter
public class PacketReceiveEvent extends PacketEvent implements Cancelable {

    private final Channel sender;

    public PacketReceiveEvent(Channel sender, CloudboundPacket packet) {
        super(packet);
        this.sender = sender;
    }
}
