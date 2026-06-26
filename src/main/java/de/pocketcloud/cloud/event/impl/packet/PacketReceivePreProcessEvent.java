package de.pocketcloud.cloud.event.impl.packet;

import de.pocketcloud.cloud.event.Cancelable;
import de.pocketcloud.cloud.event.impl.network.NetworkEvent;
import de.pocketcloud.cloud.network.NetworkNettyServer;
import de.pocketcloud.cloud.network.client.ServerClient;
import lombok.Getter;

public final class PacketReceivePreProcessEvent extends NetworkEvent implements Cancelable {

    @Getter
    private final ServerClient sender;
    @Getter
    private final String buffer;
    @Getter
    private final boolean encryption;

    public PacketReceivePreProcessEvent(NetworkNettyServer network, ServerClient sender, String buffer, boolean encryption) {
        super(network);
        this.sender = sender;
        this.buffer = buffer;
        this.encryption = encryption;
    }
}
