package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.impl.network.NetworkEvent;
import io.netty.channel.Channel;
import lombok.Getter;

@Getter
public class PacketReceiveUnknownEvent extends NetworkEvent {

    private final Channel sender;
    private final String buffer;
    private final byte[] rawBytes;
    private final int length;
    private final boolean encryption;

    public PacketReceiveUnknownEvent(Channel sender, String buffer, byte[] rawBytes, int length, boolean encryption) {
        this.sender = sender;
        this.buffer = buffer;
        this.rawBytes = rawBytes;
        this.length = length;
        this.encryption = encryption;
    }
}