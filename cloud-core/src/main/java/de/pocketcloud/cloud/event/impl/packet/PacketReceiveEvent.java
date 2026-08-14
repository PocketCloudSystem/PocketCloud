package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.api.network.packet.CloudboundPacket;
import de.pocketcloud.cloud.event.Cancelable;
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