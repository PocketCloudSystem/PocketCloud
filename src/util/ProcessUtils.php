<?php

namespace pocketcloud\cloud\util;

final class ProcessUtils {

    private static array $lastCheckTime = [];
    private static array $lastCpuTime = [];
    private static ?int $clockTicks = null;
    private static ?int $cpuCores = null;

    public static function getCpuCores(): int {
        if (self::$cpuCores !== null) return self::$cpuCores;
        return self::$cpuCores = preg_match_all("/^processor/m", file_get_contents("/proc/cpuinfo"));
    }

    public static function getProcessStatus(?int $pid = null): ?array {
        $pid = $pid ?? "self";
        $status = @file("/proc/$pid/status", FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES);
        if($status === false) return null;

        $stats = [
            "rss" => 0, // current resident set size
            "rss_peak" => 0, // High Water Mark (VmHWM)/ highest VmRSS
            "size" => 0, // virtual size
            "threads" => 0 // thread count
        ];

        foreach ($status as $line) {
            if (preg_match("/^(VmRSS|VmSize|VmHWM|Threads):\s+(\d+)/", $line, $m)) {
                switch ($m[1]) {
                    case "VmRSS":
                        $stats["rss"] = ((int) $m[2]) * 1024;
                        break;
                    case "VmHWM":
                        $stats["rss_peak"] = ((int) $m[2]) * 1024;
                        break;
                    case "VmSize":
                        $stats["size"] = ((int) $m[2]) * 1024;
                        break;
                    case "Threads":
                        $stats["threads"] = (int) $m[2];
                        break;
                }
            }
        }

        return $stats;
    }

    public static function getCpuUsage(?int $pid = null): ?float {
        $pid = $pid ?? "self";
        $actualPid = $pid ?? getmypid();
        $statFile = "/proc/$pid/stat";
        if (!file_exists($statFile)) return null;
        $stat = file_get_contents($statFile);
        if ($stat === false) return null;
        $stat = preg_replace("/^.+?\)\s+/", "", $stat);
        $parts = explode(" ", $stat);

        $utime = (int) $parts[11];
        $stime = (int) $parts[12];
        $totalCpuTime = $utime + $stime;

        $currentTime = microtime(true);
        $clockTicks = self::getClockTicks();

        if (!isset(self::$lastCheckTime[$actualPid]) || !isset(self::$lastCpuTime[$actualPid])) {
            self::$lastCheckTime[$actualPid] = $currentTime;
            self::$lastCpuTime[$actualPid] = $totalCpuTime;
            return 0.0;
        }

        $timeDiff = $currentTime - self::$lastCheckTime[$actualPid];
        $cpuDiff = $totalCpuTime - self::$lastCpuTime[$actualPid];

        self::$lastCheckTime[$actualPid] = $currentTime;
        self::$lastCpuTime[$actualPid] = $totalCpuTime;

        if ($timeDiff == 0) return 0.0;
        $cpuUsage = ($cpuDiff / $clockTicks) / $timeDiff * 100;
        return round($cpuUsage, 2);
    }

    public static function getClockTicks(): int {
        if (self::$clockTicks === null) {
            $output = shell_exec("getconf CLK_TCK 2>/dev/null");
            if ($output !== null && is_numeric(trim($output))) {
                self::$clockTicks = (int) trim($output);
            } else {
                self::$clockTicks = 100;
            }
        }

        return self::$clockTicks;
    }

    public static function getCpuSnapshot(?int $pid = null): ?array {
        $pid = $pid ?? "self";
        $statFile = "/proc/$pid/stat";
        if (!file_exists($statFile)) return null;

        $stat = file_get_contents($statFile);
        if ($stat === false) return null;

        $stat = preg_replace("/^.+?\)\s+/", "", $stat);
        $parts = explode(" ", $stat);

        $utime = (int) $parts[11];
        $stime = (int) $parts[12];

        return [
            "utime" => $utime,
            "stime" => $stime,
            "total" => $utime + $stime,
            "timestamp" => microtime(true)
        ];
    }

    public static function calculateCpuUsageFromSnapshots(array $firstSnapshot, array $secondSnapshot): ?float {
        $timeDiff = $secondSnapshot["timestamp"] - $firstSnapshot["timestamp"];
        $cpuDiff = $secondSnapshot["total"] - $firstSnapshot["total"];

        if ($timeDiff == 0) return 0.0;

        $clockTicks = self::getClockTicks();
        $cpuUsage = ($cpuDiff / $clockTicks) / $timeDiff * 100;

        return round($cpuUsage, 2);
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