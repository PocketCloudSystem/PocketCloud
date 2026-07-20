package de.pocketcloud.bridge.task;

import de.pocketcloud.api.CloudAPI;
import de.pocketcloud.api.component.server.ICloudServer;
import de.pocketcloud.api.server.ServerStatus;
import de.pocketcloud.bridge.CloudBridge;

public final class ChangeStatusTask implements Runnable {

    @Override
    public void run() {
        ICloudServer current = CloudAPI.instance().servers().current();
        if (current.status().isInGame() || current.status().isStopping() || current.status().isOffline()) return;
        int currentPlayers = CloudBridge.instance().platformPlugin().currentPlayers();
        int maxPlayers = CloudBridge.instance().platformPlugin().maxPlayers();
        if (currentPlayers >= maxPlayers) {
            current.status(ServerStatus.FULL);
        } else {
            if (current.status().isFull()) {
                current.status(ServerStatus.ONLINE);
            }
        }
    }
}