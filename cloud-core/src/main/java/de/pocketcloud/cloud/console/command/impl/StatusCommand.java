package de.pocketcloud.cloud.console.command.impl;

import de.pocketcloud.api.network.traffic.TrafficDirection;
import de.pocketcloud.cloud.PocketCloud;
import de.pocketcloud.cloud.console.command.Command;
import de.pocketcloud.cloud.console.command.ctx.CommandContext;
import de.pocketcloud.cloud.console.command.desc.CommandDescription;
import de.pocketcloud.cloud.console.command.sender.CommandSender;
import de.pocketcloud.common.util.FormatUtils;
import de.pocketcloud.network.traffic.TrafficMonitor;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@CommandDescription(name = "status", description = "Read the cloud's performance ")
public final class StatusCommand extends Command {

    @Override
    public void prepare() {}

    @Override
    public boolean run(CommandSender sender, CommandContext ctx) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long uptime = PocketCloud.instance().uptime().toSeconds();
        int threadCount = bean.getThreadCount();
        int peakThreadCount = bean.getPeakThreadCount();
        long totalStartedThreads = bean.getTotalStartedThreadCount();
        Set<Thread> threads = Thread.getAllStackTraces().keySet();
        long procMemoryUsage = PocketCloud.instance().performanceStats().processUsedMemory();
        long procMemoryUsagePeak = PocketCloud.instance().performanceStats().processPeakUsedMemory();
        long procReservedMemory = PocketCloud.instance().performanceStats().processTotalMemory();
        long procMemoryLimit = PocketCloud.instance().performanceStats().processMaxMemory();
        double procCpuUsage = PocketCloud.instance().performanceStats().processCpuUsage();
        long sysMemoryUsage = PocketCloud.instance().performanceStats().systemUsedMemory();
        long sysMemoryUsagePeak = PocketCloud.instance().performanceStats().systemPeakUsedMemory();
        long sysTotalMemory = PocketCloud.instance().performanceStats().systemTotalMemory();
        double sysCpuUsage = PocketCloud.instance().performanceStats().systemCpuUsage();
        double tps = PocketCloud.instance().performanceStats().currentTPS();
        double avgTps = PocketCloud.instance().performanceStats().averageTPS();
        double tickUsage = PocketCloud.instance().performanceStats().tickUsage();
        int playerCount = PocketCloud.instance().players().playerCount();
        int serverCount = PocketCloud.instance().servers().serverCount();

        section(sender, "Cloud", lines -> {
            lines.put("Uptime", FormatUtils.uptime(uptime) + " §8(§c" + PocketCloud.instance().currentTick() + "§8)");
            lines.put("TPS", FormatUtils.tps(tps) + " §8(§rAvg.: §b" + FormatUtils.tps(avgTps) + "§8)");
            lines.put("Tick Usage", FormatUtils.usagePercentage(tickUsage));
            lines.put("Memory Usage", FormatUtils.bytes(procMemoryUsage) + " §8(§rPeak: §b" + FormatUtils.bytes(procMemoryUsagePeak) + "§8)");
            lines.put("Reserved Memory", FormatUtils.bytes(procReservedMemory));
            lines.put("Memory Limit", FormatUtils.bytes(procMemoryLimit));
            lines.put("CPU Usage", FormatUtils.usagePercentage(procCpuUsage));
            lines.put("", "§b" + playerCount + " player" + (playerCount == 1 ? "" : "s") + " §raccross §b" + serverCount + " server" + (serverCount == 1 ? "" : "s") + "§8.");
        });

        section(sender, "Threads", lines -> {
            lines.put("Thread Count", threadCount + " thread" + (threadCount == 1 ? "" : "s") + " §8(§rPeak: §c" + peakThreadCount + "§8)");
            lines.put("Total Started Threads", totalStartedThreads + " thread" + (totalStartedThreads == 1 ? "" : "s"));
            if (PocketCloud.instance().logSettingsConfig().debugMode())
                lines.put("Threads", "§c" + String.join("§8, §c", threads.stream().map(t -> t.getClass().getName() + "@" + t.getName()).collect(Collectors.toCollection(ArrayList::new))));
        });

        section(sender, "System", lines -> {
            lines.put("Memory Usage", FormatUtils.bytes(sysMemoryUsage) + " §8(§rPeak: §b" + FormatUtils.bytes(sysMemoryUsagePeak) + "§8)");
            lines.put("Total Memory", FormatUtils.bytes(sysTotalMemory));
            lines.put("CPU Usage", FormatUtils.usagePercentage(sysCpuUsage));
        });

        section(sender, "Traffic", lines -> {
            for (Class<? extends TrafficMonitor> entry : PocketCloud.instance().traffic().globalWindows().keySet()) {
                String name = PocketCloud.instance().traffic().name(entry);
                String formattedName = name.substring(0, 1).toUpperCase() + name.substring(1);

                String bytesIn = FormatUtils.bytes(PocketCloud.instance().traffic().totalBytes(entry, TrafficDirection.IN));
                String bytesOut = FormatUtils.bytes(PocketCloud.instance().traffic().totalBytes(entry, TrafficDirection.OUT));
                String avgBytesIn = FormatUtils.bytes(PocketCloud.instance().traffic().averageBytes(entry, TrafficDirection.IN));
                String avgBytesOut = FormatUtils.bytes(PocketCloud.instance().traffic().averageBytes(entry, TrafficDirection.OUT));

                lines.put(formattedName + " All-Time Traffic", "§a" + bytesIn + " §8(§aIN§8) §8/ §c" + bytesOut + " §8(§cOUT§8)");
                lines.put(formattedName + " Average Traffic", "§a" + avgBytesIn + "/s §8/ §c" + avgBytesOut + "/s");
            }
        });

        return true;
    }

    private void section(CommandSender sender, String name, Consumer<LinkedHashMap<String, String>> lineAdder) {
        sender.info("§8==== §c" + name);
        LinkedHashMap<String, String> lines = new LinkedHashMap<>();
        lineAdder.accept(lines);
        for (Map.Entry<String, String> entry : lines.entrySet()) {
            if (entry.getKey().isBlank()) {
                sender.info("§8| §r" + entry.getValue());
            } else sender.info("§8| §r" + entry.getKey() + "§8: §b" + entry.getValue());
        }
    }
}