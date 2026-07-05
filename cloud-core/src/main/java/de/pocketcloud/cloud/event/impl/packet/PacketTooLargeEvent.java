package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.network.packet.Packet;
import de.pocketcloud.network.traffic.TrafficDirection;
import io.netty.channel.Channel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public class PacketTooLargeEvent extends PacketEvent {

    private final Channel receiverOrSender;
    private final Packet packet;
    private final int size;
    private final TrafficDirection direction;

    public PacketTooLargeEvent(Channel receiverOrSender, @Nullable Packet packet, int size, TrafficDirection direction) {
        super(packet);
        this.receiverOrSender = receiverOrSender;
        this.packet = packet;
        this.size = size;
        this.direction = direction;
    }
}
