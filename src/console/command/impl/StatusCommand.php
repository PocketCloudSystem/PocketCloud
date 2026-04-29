<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\thread\Thread;
use pocketcloud\cloud\thread\Worker;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\FormatUtils;
use pocketcloud\cloud\util\ProcessUtils;
use pocketcloud\cloud\util\Utils;

final class StatusCommand extends Command {

    public function __construct() {
        parent::__construct("status", "Read the cloud's performance");
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand, array $flags): bool {
        [
            $uptime, $threadCount, $osThreadCount, $threads,
            $memoryUsage, $memoryPeak, $virtualReservedMemory, $memoryLimit,
            $cloudCpuUsage,
            $tps, $avgTps, $tickUsage,
            $serverCount, $playerCount
        ] = array_values(Utils::readCloudPerformanceStatus());
        $systemCpuUsage = ProcessUtils::getSystemCpuUsage();
        [$systemMemoryTotal, , $systemMemoryAvailable, , $systemMemoryUsed] = array_values(ProcessUtils::getSystemMemoryStatus());

        $this->section($sender, "Cloud", [
            "Uptime§8: §b" . FormatUtils::uptime($uptime) . " §8(§c" . PocketCloud::getInstance()->getTick() . "§8)",
            "TPS§8: §b" . FormatUtils::tps($tps) . " §8(§rAvg.: §b" . FormatUtils::tps($avgTps) . "§8)",
            "Tick Usage§8: §b" . FormatUtils::usagePercentage($tickUsage),
            "Servers§8: §b" . $serverCount . " §8(§b" . $playerCount . " players§8)",
            "Memory Usage§8: §b" . FormatUtils::bytes($memoryUsage) . " §8(§rPeak: §b" . FormatUtils::bytes($memoryPeak) . "§8)",
            "Virtual Memory Size§8: §b" . FormatUtils::bytes($virtualReservedMemory),
            "Memory Limit§8: §b" . FormatUtils::bytes($memoryLimit),
            "CPU Usage§8: §b" . FormatUtils::usagePercentage($cloudCpuUsage / ProcessUtils::getCpuCores())
        ]);

        $this->section($sender, "Threads", [
            "Thread Count§8: §b" . $threadCount . " §8(§rActual Threads: §b" . $osThreadCount . "§8)",
            "Threads§8: §c" . implode("§8, §c", array_map(fn(Thread|Worker $thread) => $thread::class, $threads))
        ]);

        $this->section($sender, "System", [
            "CPU Usage§8: §b" . FormatUtils::usagePercentage($systemCpuUsage),
            "Memory Usage§8: §b" . FormatUtils::bytes($systemMemoryUsed, $systemMemoryTotal),
            "Available Memory§8: §b" . FormatUtils::bytes($systemMemoryAvailable, $systemMemoryTotal, $percentage, true),
            "Total Memory§8: §b" . FormatUtils::bytes($systemMemoryTotal)
        ]);

        $allTimeTrafficMessages = [];
        foreach (TrafficMonitorManager::getInstance()->getAllTimeTraffic() as $trafficType => $traffic) {
            $bytesIn = FormatUtils::bytes($traffic[TrafficMonitor::REGULAR_MODE_IN]);
            $bytesOut = FormatUtils::bytes($traffic[TrafficMonitor::REGULAR_MODE_OUT]);
            $allTimeTrafficMessages[] = ucfirst($trafficType) . " All-Time-Traffic: §a" . $bytesIn . " §8(§aIN§8) §8/ §c" . $bytesOut . " §8(§cOUT§8)";
        }

        $this->section($sender, "Traffic", $allTimeTrafficMessages);

        return true;
    }

    private function section(ICommandSender $sender, string $name, array $lines): void {
        $sender->info("§8==== §c$name");
        foreach ($lines as $line) {
            $sender->info("§8|§r " . $line);
        }
    }
}