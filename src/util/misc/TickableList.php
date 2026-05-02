<?php

namespace pocketcloud\cloud\util\misc;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\util\benchmark\Benchmark;

final class TickableList {

    private const float SLOW_TICKABLE_THRESHOLD_SECONDS = 0.25;
    private const float SLOW_TICKABLE_LOG_INTERVAL_SECONDS = 5.0;

    /** @var array<Tickable> */
    private static array $tickableList = [];
    /** @var array<string, float> */
    private static array $lastSlowTickableLog = [];

    public static function add(Tickable ...$instances): void {
        foreach ($instances as $instance) self::$tickableList[] = $instance;
    }

    public static function clear(): void {
        self::$tickableList = [];
        self::$lastSlowTickableLog = [];
    }

    public static function tickAll(int $currentTick): void {
        foreach (self::$tickableList as $instance) {
            $class = $instance::class;
            $start = microtime(true);
            Benchmark::startTiming($class);
            $instance->tick($currentTick);
            Benchmark::stopTiming($class);

            $duration = microtime(true) - $start;
            if ($duration >= self::SLOW_TICKABLE_THRESHOLD_SECONDS) {
                $lastLog = self::$lastSlowTickableLog[$class] ?? 0.0;
                if (($start - $lastLog) >= self::SLOW_TICKABLE_LOG_INTERVAL_SECONDS) {
                    self::$lastSlowTickableLog[$class] = $start;
                    CloudLogger::get()->warn("Slow cloud tickable Â§b{} Â§rtook Â§e{}ms Â§ron tick Â§b{}Â§r.", $class, number_format($duration * 1000, 2), (string) $currentTick);
                }
            }
        }
    }
}