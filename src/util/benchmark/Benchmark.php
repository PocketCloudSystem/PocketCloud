<?php

namespace pocketcloud\cloud\util\benchmark;

use Closure;
use RuntimeException;

final class Benchmark {

    /** @var array<array<BenchmarkTiming>> */
    private static array $timings = [];

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
        return $timing;
    }

    public static function stopTiming(string $name): BenchmarkTiming {
        if (!isset(self::$timings[$name]) || empty(self::$timings[$name])) throw new RuntimeException("No timings started for '$name'");
        $lastIndex = count(self::$timings[$name]) - 1;
        ($timing = self::$timings[$name][$lastIndex])->stopTiming();
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
            $durations = array_map(fn(BenchmarkTiming $v) => $v->getDuration(), array_filter(self::$timings[$key] ?? [], fn(BenchmarkTiming $v) => $v->isDone()));
            if (empty($durations)) continue;
            $summary[$key] = new BenchmarkTimingsSummary($key, count($durations), array_sum($durations) / count($durations), min($durations), max($durations));
        }

        return $name !== null ? ($summary[$name] ?? null) : $summary;
    }

    public static function reset(): void {
        self::$timings = [];
    }
}