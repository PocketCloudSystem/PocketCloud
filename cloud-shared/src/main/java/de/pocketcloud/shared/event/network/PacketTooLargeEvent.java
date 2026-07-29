package de.pocketcloud.shared.event.network;

import de.pocketcloud.api.network.packet.Packet;
import de.pocketcloud.api.network.traffic.TrafficDirection;
import io.netty.channel.Channel;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public final class PacketTooLargeEvent extends PacketEvent {

    private final Channel senderOrReceiver;
    private final int length;
    private final TrafficDirection direction;

    public PacketTooLargeEvent(Channel senderOrReceiver, @Nullable Packet packet, int length, TrafficDirection direction) {
        super(packet);
        this.senderOrReceiver = senderOrReceiver;
        this.length = length;
        this.direction = direction;
    }

    @Nullable
    public Packet getPacket() {
        return packet;
    }
}