package de.pocketcloud.bridge.task;

import de.pocketcloud.bridge.CloudBridge;
import de.pocketcloud.bridge.cache.RandomCache;
import de.pocketcloud.common.cache.LocalCache;

public final class ServerTimeoutTask implements Runnable {

    private final int timeout = CloudBridge.instance().environmentConfig().localServerTimeout() * 1000;

    @Override
    public void run() {
        long lastKeepAlive = LocalCache.get(RandomCache.class).get(RandomCache.KEY_LAST_KEEP_ALIVE, Long.class).orElseThrow(() -> new IllegalStateException("Started task too early"));
        if ((lastKeepAlive + timeout) <= System.currentTimeMillis()) {
            CloudBridge.instance().logger().warn("Server timed out, shutting down...");
            CloudBridge.instance().shutdown();
        }
    }
}