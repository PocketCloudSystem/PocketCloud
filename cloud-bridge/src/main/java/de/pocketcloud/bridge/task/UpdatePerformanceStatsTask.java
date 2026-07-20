package de.pocketcloud.bridge.task;

import de.pocketcloud.bridge.CloudBridge;

public final class UpdatePerformanceStatsTask implements Runnable {

    @Override
    public void run() {
        CloudBridge.instance().performanceStats().updateStats();
    }
}