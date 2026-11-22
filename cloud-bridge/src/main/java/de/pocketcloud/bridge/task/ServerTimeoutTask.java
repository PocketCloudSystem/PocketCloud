package de.pocketcloud.bridge.task;

import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.cache.RandomCache;
import de.pocketcloud.common.cache.LocalCache;

public final class ServerTimeoutTask implements Runnable {

    private final int timeout = CloudBridge.instance().environmentConfig().localServerTimeout() * 1000;

    @Override
    public void run() {
        if (CloudBridge.instance().reconnecting() || CloudBridge.instance().stopping()) return;
        long lastKeepAlive = LocalCache.get(RandomCache.class).get(RandomCache.KEY_LAST_KEEP_ALIVE, Long.class).orElseThrow(() -> new IllegalStateException("Started task too early"));
        if ((lastKeepAlive + timeout) <= System.currentTimeMillis()) {
            CloudBridge.instance().logger().warn("No keep alive from the cloud, reconnecting...");
            CloudBridge.instance().connectionLost();
            CloudBridge.instance().network().disconnect();
        }
    }
}