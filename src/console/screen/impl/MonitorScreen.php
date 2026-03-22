<?php

namespace pocketcloud\cloud\console\screen\impl;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\color\CloudConsoleColor;
use pocketcloud\cloud\console\log\output\impl\MonitorOutputHandler;
use pocketcloud\cloud\console\screen\Screen;
use pocketcloud\cloud\console\screen\ScreenManager;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;
use pocketcloud\cloud\traffic\TrafficMonitorManager;
use pocketcloud\cloud\util\FormatUtils;
use pocketcloud\cloud\util\ProcessUtils;
use pocketcloud\cloud\util\Utils;

final class MonitorScreen extends Screen {

    private bool $firstCycle = true;
    private string $latestInput = "";

    public function initialize(Console $console): void {
        $this->clearConsole();
        $this->disableHistory();
        $this->hideTyping();
        $this->hideCursor();
        $this->setControlCHandler(fn() => ScreenManager::getInstance()->resetScreen());
        $this->setPrompt("");
        $this->setOutputHandler(new MonitorOutputHandler());
    }

    public function handleInput(string $input): void {
        $this->setInput($input);
    }

    public function tick(int $currentTick): void {
        $currentInput = $this->getinput();
        $doNow = $this->latestInput !== $currentInput;
        $this->latestInput = $currentInput;
        $servers = array_filter(CloudServerManager::getInstance()->getAll(), fn(CloudServer $server) => trim($currentInput) == "" || str_starts_with($server->getName(), trim($currentInput)) !== false);

        if ($currentTick % 20 === 0 || $this->firstCycle || $doNow) {
            $this->clearConsole();
            $this->firstCycle = false;

            $this->echo("§8" . ($headlineBars = str_repeat("=", 30)) . " §bCloud §8" . $headlineBars);

            [$uptime, , $threadCount, , $memoryUsage, , , $memoryLimit, $cpuUsage, $tps, $avgTps, $tickUsage, , $playerCount] = array_values(Utils::readCloudPerformanceStatus());
            [$totalAvgAllTimeTrafficIn, $totalAvgAllTimeTrafficOut] = TrafficMonitorManager::getInstance()->getTotalAllAverageTimeTraffic();
            $uptime = FormatUtils::uptime($uptime);
            $formattedMemoryUsage = FormatUtils::bytes($memoryUsage);

            $this->echo("§rUptime: §b" . str_replace("§8", "§b", $uptime) . " §8(§c{$currentTick}§8) §8| §rThreads: §c" . $threadCount);
            $this->echo("§rMemory Usage: §b" . $formattedMemoryUsage . " " . $this->drawPctBars($memoryUsage, $memoryLimit, 10, $memPct) . " " . $this->formatPercentageWithColor($memPct) . $memPct . "%");
            $this->echo("§rCPU Usage: " . $this->drawPctBars($cpuUsage / ProcessUtils::getCpuCores(), 100, 10, $cpuPct) . " " . $this->formatPercentageWithColor($cpuPct) . $cpuPct . "%");
            $this->echo("§rTPS: " . FormatUtils::tps($tps) . " §8(§rAverage: " . FormatUtils::tps($avgTps) . "§8) §8| §rTick Usage: §e" . FormatUtils::usagePercentage($tickUsage));
            $this->echo("§rPlayer Count: §b" . $playerCount . " player" . ($playerCount == 1 ? "" : "s"));
            $this->echo("§rAverage Total Traffic: §aIN §b" . FormatUtils::bytes($totalAvgAllTimeTrafficIn) . "/s §8| §cOUT §b" . FormatUtils::bytes($totalAvgAllTimeTrafficOut) . "/s");
            $this->echo("");

            $serverCount = count($servers);
            $this->echo("§8" . ($headlineBars = str_repeat("=", 30)) . " §bServers §8(§b{$serverCount}§8) §8" . $headlineBars);

            $cardsPerRow = 4;
            $maxRows = 7;
            $spacing = 3;

            $cards = [];
            foreach ($servers as $server) {
                [$name, , , , , , , , , $tps, $avgTps, $memoryUsage, , $memoryLimit, $cpuUsage] = array_values($server->write());

                $cards[] = [
                    "name" => $name,
                    "tps" => $tps,
                    "avgTps" => $avgTps,
                    "mem" => $memoryUsage,
                    "memPct" => min(100, round($memoryLimit > 0 ? ($memoryUsage * 100 / $memoryLimit) : 100)),
                    "memLimit" => $memoryLimit,
                    "cpu" => $cpuUsage,
                    "players" => $server->getPlayerCount()
                ];
            }

            $amountRows = min($maxRows, $firstAmountRows = ceil(count($cards) / $cardsPerRow));
            $cutOff = $firstAmountRows > $maxRows;

            for ($i = 0; $i < min(count($cards), $amountRows * $cardsPerRow); $i += $cardsPerRow) {
                $rowCards = array_slice($cards, $i, $cardsPerRow);

                $maxWidth = 0;
                foreach ($rowCards as $c) {
                    $line1 = "§8[§b" . $c["name"] . "§8]";
                    $line2 = "§rTPS: " . ($c["tps"] <= 0 ? "§cUnknown" : FormatUtils::tps($c["tps"], false) . " §8(" . FormatUtils::tps($c["avgTps"], false) . "§8)");
                    $line3 = "§rCPU: §b" . FormatUtils::usagePercentage($c["cpu"] / ProcessUtils::getCpuCores(), false, 0) . "% §8| §rMEM: §b" . FormatUtils::bytes($c["mem"], $c["memLimit"]) . " (" . $c["memPct"] . "%)";
                    $line4 = "§rPlayers: §b" . $c["players"];

                    $maxWidth = max($maxWidth,
                        mb_strlen(CloudConsoleColor::stripColors($line1), 'UTF-8'),
                        mb_strlen(CloudConsoleColor::stripColors($line2), 'UTF-8'),
                        mb_strlen(CloudConsoleColor::stripColors($line3), 'UTF-8'),
                        mb_strlen(CloudConsoleColor::stripColors($line4), 'UTF-8')
                    );
                }

                $line1 = "";
                foreach ($rowCards as $c) {
                    $name = $this->truncate("§8[§b" . $c["name"] . "§8]", $maxWidth);
                    $line1 .= $this->padAnsi($name, $maxWidth);
                    $line1 .= str_repeat(" ", $spacing);
                }

                $this->echo($line1);

                $line2 = "";
                foreach ($rowCards as $c) {
                    $tpsLine = "§rTPS: " . ($c["tps"] <= 0 ? "§cUnknown" : FormatUtils::tps($c["tps"], false) . " §8(" . FormatUtils::tps($c["avgTps"], false) . "§8)");
                    $line2 .= $this->padAnsi($tpsLine, $maxWidth);
                    $line2 .= str_repeat(" ", $spacing);
                }

                $this->echo($line2);

                $line3 = "";
                foreach ($rowCards as $c) {
                    $memLine = "§rCPU: §b" . FormatUtils::usagePercentage($c["cpu"] / ProcessUtils::getCpuCores(), false, 0) . " §8| §rMEM: §b" . FormatUtils::bytes($c["mem"], $c["memLimit"]);
                    $line3 .= $this->padAnsi($memLine, $maxWidth);
                    $line3 .= str_repeat(" ", $spacing);
                }
                $this->echo($line3);

                $line4 = "";
                foreach ($rowCards as $c) {
                    $pLine = "§rPlayers: §b" . $c["players"];
                    $line4 .= $this->padAnsi($pLine, $maxWidth);
                    $line4 .= str_repeat(" ", $spacing);
                }
                $this->echo($line4);

                $this->echo("");
            }

            $this->echo("");
            $this->echo("§lPress CTRL + C to exit." . ($cutOff ? " Some entries have been cut off, please type server names to filter." : ""));
        }
    }

    private function drawPctBars(float $value, float $max, int $barsCount, ?float &$pct = null, ?float &$leftOverPct = null): string {
        $pct = min(100, round($max > 0 ? ($value * 100 / $max) : 100));
        $leftOverPct = 100 - $pct;
        $occupiedBarsCount = floor(($pct / $barsCount) / 10);
        $freeBarsCount = floor(abs(($leftOverPct - $pct) / $barsCount));
        return "§8[§c" . str_repeat("|", $occupiedBarsCount) . "§a" . str_repeat("=", $freeBarsCount) . "§r§8]";
    }

    private function formatPercentageWithColor(float $percentage, bool $lowerBetter = true): string {
        return match (true) {
            $percentage < 60 => ($lowerBetter ? "§a" : "§c"),
            $percentage < 85 => "§e",
            default => ($lowerBetter ? "§c" : "§a")
        };
    }

    private function truncate(string $text, int $maxLength): string {
        if (strlen($text) <= $maxLength) return $text;
        return substr($text, 0, $maxLength - 1) . "…";
    }

    private function padAnsi(string $text, int $width): string {
        $clean = CloudConsoleColor::stripColors($text);
        $len = mb_strlen($clean, "UTF-8");

        if ($len >= $width) return $text;
        return $text . str_repeat(" ", $width - $len);
    }

    public function onRemove(int $currentTick): void {
        $this->clearConsole();
        $this->enableHistory();
        $this->showTyping();
        $this->showCursor();
        $this->setInput("");
        $this->restoreAll();
        $this->resetOutputManager();
        $this->printLogCache();
    }

    private function echo(string $message): void {
        CloudLogger::get()->echo(CloudConsoleColor::toColoredString($message . "§r"));
    }
}