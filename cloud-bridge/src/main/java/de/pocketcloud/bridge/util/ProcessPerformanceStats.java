package de.pocketcloud.bridge.util;

import com.sun.management.OperatingSystemMXBean;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.lang.management.ManagementFactory;

@Getter
@Accessors(fluent = true)
public final class ProcessPerformanceStats {

    private static final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

    private long lastUpdate = 0;
    private long maxMemory = 0;
    private long totalMemory = 0;
    private long freeMemory = 0;
    private long usedMemory = 0;
    private long peakUsedMemory = 0;
    private double cpuUsage = 0;

    public void updateStats() {
        lastUpdate = System.currentTimeMillis();
        Runtime runtime = Runtime.getRuntime();
        maxMemory = runtime.maxMemory();
        totalMemory = runtime.totalMemory();
        freeMemory = runtime.freeMemory();
        usedMemory = totalMemory - freeMemory;
        peakUsedMemory = Math.max(peakUsedMemory, usedMemory);
        if ((System.currentTimeMillis() - lastUpdate) >= 2000) {
            cpuUsage = Math.max(0, osBean.getCpuLoad() * 100);
        }
    }
}