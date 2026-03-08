<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\traffic\TrafficMonitor;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\FormatUtils;
use pocketcloud\cloud\util\ProcessUtils;
use pocketcloud\cloud\util\Utils;

final class StatusCommand extends Command {

    public function __construct() {
        parent::__construct("status", "Read the cloud's performance");
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        foreach (explode(
            "\n",
            FormatUtils::implodeWithKeys(
                Utils::readCloudPerformanceStatus(),
                "\n",
                "§8: §b",
                fn(string $key) => trim(implode(" ", array_map(fn(string $key) => ucfirst($key), explode(" ", str_replace(["vm_size", "_", "vm", "rss"], ["virtual_memory_reserved_size", " ", "", "memory usage"], $key))))),
                function (string $key, mixed $value): mixed {
                    if (in_array($key, [
                        "vm_rss", "vm_size", "vm_rss_peak", "memory_limit"
                    ])) {
                        return FormatUtils::bytes(intval($value));
                    } else if (in_array($key, [
                        "current_tps", "average_tps"
                    ])) {
                        return FormatUtils::tps($value);
                    } else if (is_array($value) && $key == "threads") {
                        return "§c" . implode("§8, §c", array_map(fn(object $obj) => $obj::class, $value));
                    } else if (in_array($key, ["tick_usage", "cpu_usage"])) {
                        return FormatUtils::usagePercentage($key == "cpu_usage" ? ($value / ProcessUtils::getCpuCores()) : $value);
                    } else if ($key == "uptime") {
                        return FormatUtils::uptime($value) . " §8(§c" . Server::getInstance()->getTick() . "§8)";
                    }

                    return $value;
                }
            )
        ) as $line) {
            $sender->success($line);
        }

        $allTimeTrafficMessages = [];
        foreach (TrafficMonitorManager::getInstance()->getAllTimeTraffic() as $trafficType => $traffic) {
            $bytesIn = FormatUtils::bytes($traffic[TrafficMonitor::REGULAR_MODE_IN]);
            $bytesOut = FormatUtils::bytes($traffic[TrafficMonitor::REGULAR_MODE_OUT]);
            $allTimeTrafficMessages[] = ucfirst($trafficType) . " All-Time-Traffic: §a" . $bytesIn . " §8(§aIN§8) §8/ §c" . $bytesOut . " §8(§cOUT§8)";
        }

        foreach ($allTimeTrafficMessages as $message) {
            $sender->success($message);
        }

        return true;
    }
}