package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.impl.network.NetworkEvent;
import io.netty.channel.Channel;
import lombok.Getter;

@Getter
public class PacketReceiveUnknownEvent extends NetworkEvent {

    private final Channel sender;
    private final byte[] payload;
    private final int length;
    private final boolean encryption;

    public PacketReceiveUnknownEvent(Channel sender, byte[] payload, int length, boolean encryption) {
        this.sender = sender;
        this.payload = payload;
        this.length = length;
        this.encryption = encryption;
    }
}