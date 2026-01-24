<?php

namespace pocketcloud\cloud\util;

final class ProcessUtils {

    private static array $cycleSnapshots = [];
    private static array $latestResults = [];

    private static ?int $clockTicks = null;
    private static ?int $cpuCores = null;

    public static function startCpuRetrieveCycle(?int $pid = null): void {
        $actualPid = $pid ?? getmypid();
        $snapshot = self::getCpuSnapshot($actualPid);
        if ($snapshot !== null) {
            self::$cycleSnapshots[$actualPid] = $snapshot;
        }
    }

    public static function stopCpuRetrieveCycle(?int $pid = null): void {
        $actualPid = $pid ?? getmypid();
        if (!isset(self::$cycleSnapshots[$actualPid])) return;
        $firstSnapshot = self::$cycleSnapshots[$actualPid];
        $secondSnapshot = self::getCpuSnapshot($actualPid);
        if ($secondSnapshot === null) return;
        $usage = self::calculateCpuUsageFromSnapshots($firstSnapshot, $secondSnapshot);
        self::$latestResults[$actualPid] = $usage;
    }

    public static function restartCpuRetrieveCycle(?int $pid = null): void {
        $actualPid = $pid ?? getmypid();
        self::stopCpuRetrieveCycle($actualPid);
        self::startCpuRetrieveCycle($actualPid);
    }

    public static function getCpuUsage(?int $pid = null): float {
        $actualPid = $pid ?? getmypid();
        return self::$latestResults[$actualPid] ?? 0.0;
    }

    public static function getCpuSnapshot(?int $pid = null): ?array {
        $pid = $pid ?? "self";
        $statFile = "/proc/$pid/stat";
        if (!file_exists($statFile)) return null;
        $stat = file_get_contents($statFile);
        if ($stat === false) return null;
        $parts = explode(" ", substr($stat, strrpos($stat, ")") + 2));
        $utime = (int) $parts[11];
        $stime = (int) $parts[12];
        return [
            "total" => $utime + $stime,
            "timestamp" => microtime(true)
        ];
    }

    public static function getProcessStatus(?int $pid = null): ?array {
        $pid = $pid ?? "self";
        $handle = @fopen("/proc/$pid/status", "r");
        if (!$handle) return null;

        $stats = [
            "rss" => 0,      // current resident set size
            "rss_peak" => 0, // High Water Mark
            "size" => 0,     // virtual size
            "threads" => 0   // thread count
        ];

        $found = 0;
        while (($line = fgets($handle)) !== false && $found < 4) {
            if (!str_starts_with($line, "Vm") && !str_starts_with($line, "Th")) continue;
            if (preg_match("/^(VmRSS|VmSize|VmHWM|Threads):\s+(\d+)/", $line, $m)) {
                $value = (int) $m[2];
                match ($m[1]) {
                    "VmRSS" => $stats["rss"] = $value * 1024,
                    "VmHWM" => $stats["rss_peak"] = $value * 1024,
                    "VmSize" => $stats["size"] = $value * 1024,
                    "Threads" => $stats["threads"] = $value,
                    default => null
                };
                $found++;
            }
        }

        fclose($handle);
        return $stats;
    }

    public static function calculateCpuUsageFromSnapshots(array $firstSnapshot, array $secondSnapshot): float {
        $timeDiff = $secondSnapshot["timestamp"] - $firstSnapshot["timestamp"];
        $cpuDiff = $secondSnapshot["total"] - $firstSnapshot["total"];

        if ($timeDiff <= 0 || $cpuDiff < 0) return 0.0;

        $usage = ($cpuDiff / self::getClockTicks()) / $timeDiff * 100;
        return round($usage, 2);
    }

    public static function getClockTicks(): int {
        if (self::$clockTicks !== null) return self::$clockTicks;
        $output = shell_exec("getconf CLK_TCK 2>/dev/null");
        self::$clockTicks = (int) ($output !== null && is_numeric(trim($output)) ? trim($output) : 100);
        return self::$clockTicks;
    }

    public static function getCpuCores(): int {
        if (self::$cpuCores !== null) return self::$cpuCores;
        return self::$cpuCores = preg_match_all("/^processor/m", file_get_contents("/proc/cpuinfo"));
    }

    public static function getMemoryLimit(): int {
        $memoryLimit = ini_get("memory_limit");
        if ($memoryLimit == -1) return -1;
        if (is_numeric($memoryLimit)) return (int) $memoryLimit;
        $unit = strtoupper(substr($memoryLimit, -1));
        $value = (int) substr($memoryLimit, 0, -1);

        return match ($unit) {
            "G" => $value * 1024 * 1024 * 1024,
            "M" => $value * 1024 * 1024,
            "K" => $value * 1024,
            default => (int)$memoryLimit,
        };
    }

    public static function kill(int $pid, bool $subprocesses = true): void {
        if ($subprocesses) $pid = -$pid;

        if (function_exists("posix_kill")) {
            posix_kill($pid, 9);
        } else {
            exec("kill -9 $pid > /dev/null 2>&1");
        }
    }
}