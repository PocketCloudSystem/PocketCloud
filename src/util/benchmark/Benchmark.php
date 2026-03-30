<?php

namespace pocketcloud\cloud\util\benchmark;

use Closure;
use RuntimeException;

final class Benchmark {

    /** @var array<BenchmarkTiming> */
    private static array $timings = [];
    private static array $timingsSummary = [];

    public static function measure(Closure $fn, int $iterations = 1, ?string $name = null): BenchmarkResult {
        $times = [];
        for ($i = 0; $i < $iterations; $i++) {
            $start = hrtime(true);
            $fn();
            $end = hrtime(true);
            $times[] = ($end - $start) / 1_000_000;
        }

        return new BenchmarkResult($name, $iterations, array_sum($times) / count($times), min($times), max($times));
    }

    public static function startTiming(string $name): BenchmarkTiming {
        self::$timings[$name] = ($timing = new BenchmarkTiming($name));
        $timing->startTiming();
        return $timing;
    }

    public static function stopTiming(string $name): BenchmarkTiming {
        if (!isset(self::$timings[$name])) throw new RuntimeException("No timings started for '$name'");
        ($timing = self::$timings[$name])->stopTiming();
        unset(self::$timings[$name]);

        if (!isset(self::$timingsSummary[$name])) {
            self::$timingsSummary[$name] = [
                "count" => 0,
                "sum" => 0,
                "min" => PHP_FLOAT_MAX,
                "max" => 0
            ];
        }

        self::$timingsSummary[$name]["count"] += 1;
        self::$timingsSummary[$name]["sum"] += $timing->getDuration();
        self::$timingsSummary[$name]["min"] = min($timing->getDuration(), self::$timingsSummary[$name]["min"]);
        self::$timingsSummary[$name]["max"] = max($timing->getDuration(), self::$timingsSummary[$name]["max"]);

        return $timing;
    }

    public static function getSummary(?string $name = null): array|BenchmarkTimingsSummary|null {
        $summary = [];
        $keys = $name !== null ? [$name] : array_keys(self::$timingsSummary);

        foreach ($keys as $key) {
            [$count, $sum, $min, $max] = array_values(self::$timingsSummary[$key]);
            $summary[$key] = new BenchmarkTimingsSummary($key, $count, $sum / $count, $min, $max);
        }

        return $name !== null ? ($summary[$name] ?? null) : $summary;
    }

    public static function reset(): void {
        self::$timings = [];
    }
}