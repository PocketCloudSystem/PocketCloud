package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.event.impl.network.NetworkEvent;
import io.netty.channel.Channel;
import lombok.Getter;

@Getter
public class PacketReceivePreProcessEvent extends NetworkEvent implements Cancelable {

    private final Channel sender;
    private final byte[] payload;
    private final boolean encryption;

    public PacketReceivePreProcessEvent(Channel sender, byte[] payload, boolean encryption) {
        this.sender = sender;
        this.payload = payload;
        this.encryption = encryption;
    }
}
