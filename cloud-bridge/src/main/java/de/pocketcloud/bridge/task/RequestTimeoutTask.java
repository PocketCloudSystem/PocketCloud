package de.pocketcloud.bridge.task;

import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.network.packet.RequestPacket;

public final class RequestTimeoutTask implements Runnable {

    private final int timeout = CloudBridge.instance().environmentConfig().localServerTimeout() * 1000;

    @Override
    public void run() {
        for (RequestPacket packet : CloudBridge.instance().requests().getAll().values()) {
            if ((packet.getSentTimestamp() + timeout) <= System.currentTimeMillis()) {
                CloudBridge.instance().requests().reject(packet);
            }
        }
    }
}