package de.pocketcloud.shared.event.network;

import io.netty.channel.Channel;
import lombok.Getter;

@Getter
public final class UnknownPacketReceivedEvent extends PacketEvent {

    private final Channel sender;
    private final byte[] payload;
    private final int length;

    public UnknownPacketReceivedEvent(Channel sender, byte[] payload, int length) {
        super(null);
        this.sender = sender;
        this.payload = payload;
        this.length = length;
    }
}