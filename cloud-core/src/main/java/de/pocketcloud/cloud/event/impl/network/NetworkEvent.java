package de.pocketcloud.cloud.event.impl.network;

import de.pocketcloud.cloud.event.Event;
import de.pocketcloud.cloud.network.NetworkNettyServer;
import lombok.Getter;

public abstract class NetworkEvent extends Event {

    @Getter
    private final NetworkNettyServer network;

    public NetworkEvent(NetworkNettyServer network) {
        this.network = network;
    }
}
