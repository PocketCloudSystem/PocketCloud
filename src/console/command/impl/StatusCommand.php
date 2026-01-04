<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\console\log\CloudLogger;
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
                fn(string $key) => implode(" ", array_map(fn(string $key) => ucfirst($key), explode(" ", str_replace(["_", "vm", "rss"], [" ", "virtual memory", "physical memory usage"], $key)))),
                function (string $key, mixed $value): mixed {
                    if (in_array($key, [
                        "vm_rss", "vm_size", "vm_peak",
                        "php_memory_usage", "php_memory_peak"
                    ])) {
                        return FormatUtils::bytes(intval($value));
                    } else if (in_array($key, [
                        "current_tps", "average_tps"
                    ])) {
                        return ($value >= 19 ? "§a" : ($value >= 17 ? "§6" : "§c")) . round($value, 2);
                    } else if (is_array($value) && $key == "threads") {
                        return "§c" . implode("§8, §c", array_map(fn(object $obj) => $obj::class, $value));
                    } else if ($key == "tick_usage") {
                        return round($value, 2) . "%";
                    }

                    return $value;
                }
            )
        ) as $line) {
            $sender->success($line);
        }

        return true;
    }
}