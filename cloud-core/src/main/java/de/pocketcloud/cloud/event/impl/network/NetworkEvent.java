package de.pocketcloud.cloud.event.impl.network;

import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.event.CloudEvent;
import de.pocketcloud.cloud.network.NetworkNettyServer;

public abstract class NetworkEvent extends CloudEvent {

    public NetworkNettyServer getNetwork() {
        return PocketCloud.instance().network();
    }
}