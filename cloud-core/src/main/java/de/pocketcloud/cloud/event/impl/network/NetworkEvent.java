package de.pocketcloud.cloud.event.impl.network;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.event.Event;
import de.pocketcloud.cloud.network.NetworkNettyServer;
import lombok.Getter;

public abstract class NetworkEvent extends Event {

    public NetworkNettyServer getNetwork() {
        return PocketCloud.instance().network();
    }
}