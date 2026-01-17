<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\util\FormatUtils;
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
                "§r: §b",
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
                        return FormatUtils::usagePercentage($value, $key == "tick_usage");
                    } else if ($key == "uptime") {
                        return $this->formatUptime($value);
                    }

                    return $value;
                }
            )
        ) as $line) {
            $sender->success($line);
        }

        return true;
    }

    private function formatUptime(float $seconds): string {
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
}