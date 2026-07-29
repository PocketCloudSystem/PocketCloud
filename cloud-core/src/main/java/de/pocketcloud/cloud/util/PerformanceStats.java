package de.pocketcloud.cloud.util;

import com.sun.management.OperatingSystemMXBean;
import lombok.Getter;
import lombok.experimental.Accessors;
import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

@Getter
@Accessors(fluent = true)
public final class PerformanceStats {

    private static final OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final SystemInfo systemInfo = new SystemInfo();

    private final List<Double> tickTimes = new ArrayList<>();
    private double tickTimesSum = 0;
    private double lastTickTime = 0;
    private double currentTPS = 0;
    private double averageTPS = 0;
    private double tickUsage = 0;

    private long processMaxMemory = 0;
    private long processTotalMemory = 0;
    private long processFreeMemory = 0;
    private long processUsedMemory = 0;
    private long processPeakUsedMemory = 0;
    private double processCpuUsage = 0;
    private long systemTotalMemory = 0;
    private long systemFreeMemory = 0;
    private long systemUsedMemory = 0;
    private long systemPeakUsedMemory = 0;
    private double systemCpuUsage = 0;
    private final int availableProcessors = osBean.getAvailableProcessors();

    public void updateTickStats(double tickStart, double tickEnd) {
        double timeSinceLastTick = tickStart - lastTickTime;
        lastTickTime = tickStart;

        if (timeSinceLastTick > 0) {
            currentTPS = Math.min(20.0, 1 / timeSinceLastTick);
        }

        tickTimes.add(timeSinceLastTick);
        tickTimesSum += timeSinceLastTick;
        if (tickTimes.size() > 20) {
            tickTimesSum -= tickTimes.removeFirst();
        }

        double avgTickTime = tickTimesSum / tickTimes.size();
        averageTPS = avgTickTime > 0 ? Math.min(20, 1 / avgTickTime) : 20;

        tickUsage = Math.min(100, (tickEnd - tickStart) / 2000);
    }

    public void updateSystemStats(long tick) {
        Runtime runtime = Runtime.getRuntime();
        processMaxMemory = runtime.maxMemory();
        processTotalMemory = runtime.totalMemory();
        processFreeMemory = runtime.freeMemory();
        processUsedMemory = processTotalMemory - processFreeMemory;
        processPeakUsedMemory = Math.max(processPeakUsedMemory, processUsedMemory);
        if (tick % 40 == 0) {
            GlobalMemory memory = systemInfo.getHardware().getMemory();
            processCpuUsage = Math.max(0, osBean.getProcessCpuLoad() * 100);
            systemTotalMemory = memory.getTotal();
            systemFreeMemory = memory.getAvailable();
            systemUsedMemory = systemTotalMemory - systemFreeMemory;
            systemPeakUsedMemory = Math.max(systemPeakUsedMemory, systemUsedMemory);
            systemCpuUsage = Math.max(0, osBean.getCpuLoad() * 100);
        }
    }
}