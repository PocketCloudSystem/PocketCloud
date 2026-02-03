<?php

namespace pocketcloud\cloud\util\benchmark;

use Closure;
use RuntimeException;

final class Benchmark {

    private const int MAX_TIMINGS = 100;

    /** @var array<array<BenchmarkTiming>> */
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
        self::$timings[$name][] = ($timing = new BenchmarkTiming($name));
        $timing->startTiming();

        if (count(self::$timings[$name]) > self::MAX_TIMINGS) array_shift(self::$timings[$name]);

        return $timing;
    }

    public static function stopTiming(string $name): BenchmarkTiming {
        if (!isset(self::$timings[$name]) || empty(self::$timings[$name])) throw new RuntimeException("No timings started for '$name'");
        ($timing = self::$timings[$name][$index = count(self::$timings[$name]) - 1])->stopTiming();
        unset(self::$timings[$name][$index]);

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

    public static function getTimings(?string $name = null): array {
        if ($name !== null) return self::$timings[$name] ?? [];
        return self::$timings;
    }

    public static function getSummary(?string $name = null): array|BenchmarkTimingsSummary|null {
        $summary = [];
        $keys = $name !== null ? [$name] : array_keys(self::$timings);

        foreach ($keys as $key) {
            if (!isset(self::$timingsSummary[$key])) continue;
            [$count, $sum, $min, $max] = array_values(self::$timingsSummary[$key]);
            $summary[$key] = new BenchmarkTimingsSummary($key, $count, $sum / $count, $min, $max);
        }

        return $name !== null ? ($summary[$name] ?? null) : $summary;
    }

    public static function reset(): void {
        self::$timings = [];
    }
}