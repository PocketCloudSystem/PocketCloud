<?php

namespace pocketcloud\cloud\command\impl;

use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\thread\Thread;
use pocketcloud\cloud\thread\Worker;
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

        $threadNames = array_map(fn(Thread|Worker $thread) => $thread::class, $threads);

        $sender->info("Current §bPocket§3Cloud §rperformance status:");
        $sender->info("Uptime: §c" . $this->formatUptime());
        $sender->info("Thread Count: §c" . $threadCount . " §8[§e" . implode("§8, §e", $threadNames) . "§8]");
        $sender->info("Main thread memory: §c" . round(($mainMemory / 1024) / 1024, 2) . " MB");
        $sender->info("Main thread memory peak: §c" . round(($mainMemoryPeak / 1024) / 1024, 2) . " MB");
        $sender->info("Total memory: §c" . round(($mainMemorySys / 1024) / 1024, 2) . " MB");
        $sender->info("Total memory peak: §c" . round(($mainMemorySysPeak / 1024) / 1024, 2) . " MB");
        if ($memoryLimit > 0) $sender->info("Memory limit: §c" . round($memoryLimit, 2) . " MB");
        $sender->info("Server count: §c" . $serverCount . " server" . ($serverCount == 1 ? "" : "s"));
        $sender->info("Player count: §c" . $playerCount . " player" . ($playerCount == 1 ? "" : "s"));
        return true;
    }

    private function formatUptime(): string {
        $seconds = PocketCloud::getInstance()->getUptime();
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