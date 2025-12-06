<?php

namespace pocketcloud\cloud\command\impl;

use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\thread\Thread;
use pocketcloud\cloud\thread\Worker;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\Utils;

final class StatusCommand extends Command {

    public function __construct() {
        parent::__construct("status", "View the cloud's performance");
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        [
            $threadCount,
            $threads,
            $mainMemory,
            $mainMemoryPeak,
            $mainMemorySys,
            $mainMemorySysPeak,
            $memoryLimit,
            $serverCount,
            $playerCount
        ] = Utils::readCloudPerformanceStatus();

        $trafficMessages = [];
        foreach (TrafficMonitorManager::getInstance()->getAllTimeTraffic() as $trafficType => $traffic) {
            $bytesIn = $this->formatBytes($traffic[TrafficMonitor::REGULAR_MODE_IN]);
            $bytesOut = $this->formatBytes($traffic[TrafficMonitor::REGULAR_MODE_OUT]);
            $bytesAvgIn = $this->formatBytes($traffic[TrafficMonitor::REGULAR_MODE_IN . TrafficMonitor::SUFFIX_AVG]);
            $bytesAvgOut = $this->formatBytes($traffic[TrafficMonitor::REGULAR_MODE_OUT . TrafficMonitor::SUFFIX_AVG]);

            $activeMonitors = array_values(array_filter(TrafficMonitorManager::getInstance()->getTrafficMonitors($trafficType) ?? [], fn(TrafficMonitor $monitor) => $monitor->isActive()));
            usort($activeMonitors, fn(TrafficMonitor $a, TrafficMonitor $b) => $a->getMonitoringDuration() <=> $b->getMonitoringDuration());
            $countActiveMonitors = count($activeMonitors);

            $trafficMessages[] = "§8---- §cTraffic Monitor: §b" . mb_strtoupper($trafficType) . " §8----";
            $trafficMessages[] = "Active Monitors: §b" . $countActiveMonitors . " monitor" . ($countActiveMonitors == 1 ? "" : "s");
            if ($countActiveMonitors > 0) $trafficMessages[] = "Current longest active monitor: §c" . $this->formatUptime($activeMonitors[0]->getMonitoringDuration());
            $trafficMessages[] = "All-Time-Traffic: §a" . $bytesIn . " §8(§aIN§8) §8/ §c" . $bytesOut . " §8(§cOUT§8)";
            $trafficMessages[] = "All-Time Average Traffic: §a" . $bytesAvgIn . "/s §8(§aIN§8) §8/ §c" . $bytesAvgOut . "/s §8(§cOUT§8)";
        }

        $threadNames = array_map(fn(Thread|Worker $thread) => $thread::class, $threads);

        $sender->info("Current §bPocket§3Cloud §rperformance status:");
        $sender->info("Uptime: §c" . $this->formatUptime());
        $sender->info("Thread Count: §c" . $threadCount . " §8[§e" . implode("§8, §e", $threadNames) . "§8]");
        $sender->info("Main thread memory: §c" . $this->formatBytes($mainMemory));
        $sender->info("Main thread memory peak: §c" . $this->formatBytes($mainMemoryPeak));
        $sender->info("Total memory: §c" . $this->formatBytes($mainMemorySys));
        $sender->info("Total memory peak: §c" . $this->formatBytes($mainMemorySysPeak));
        if ($memoryLimit > 0) $sender->info("Memory limit: §c" . round($memoryLimit, 2) . " MB");
        $sender->info("Server count: §c" . $serverCount . " server" . ($serverCount == 1 ? "" : "s"));
        $sender->info("Player count: §c" . $playerCount . " player" . ($playerCount == 1 ? "" : "s"));
        foreach ($trafficMessages as $message) {
            $sender->info($message);
        }

        return true;
    }

    private function formatUptime(?int $seconds = null): string {
        $seconds = $seconds ?? PocketCloud::getInstance()->getUptime();
        $days = 0;
        $hours = 0;
        $minutes = 0;

        while ($seconds >= 86400) {
            $days++;
            $seconds -= 86400;
        }

        while ($seconds >= 3600) {
            $hours++;
            $seconds -= 3600;
        }

        while ($seconds >= 60) {
            $minutes++;
            $seconds -= 60;
        }

        return ($days > 0 ? $days . "d, " : "") .
            ($hours > 0 ? $hours . "h, " : "") .
            ($minutes > 0 ? $minutes . "m, " : "") .
            ($seconds > 0 ? floor($seconds) . "s" : "");
    }

    private function formatBytes(int $bytes): string {
        if ($bytes < 1024) return $bytes . " B";

        $units = ["B", "KB", "MB", "GB", "TB", "PB", "EB"];
        $exp = (int) floor(log($bytes, 1024));
        $value = $bytes / (1024 ** $exp);
        $value = round($value, 2);

        return $value . " " . $units[$exp];
    }
}