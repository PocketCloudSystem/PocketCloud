<?php

namespace pocketcloud\cloud\util\misc;

use pocketcloud\cloud\util\benchmark\Benchmark;

final class TickableList {

    /** @var array<Tickable> */
    private static array $tickableList = [];

    public static function add(Tickable ...$instances): void {
        foreach ($instances as $instance) self::$tickableList[] = $instance;
    }

    public static function clear(): void {
        self::$tickableList = [];
    }

    public static function tickAll(int $currentTick): void {
        foreach (self::$tickableList as $instance) {
            Benchmark::startTiming($instance::class);
            $instance->tick($currentTick);
            Benchmark::stopTiming($instance::class);
        }
    }
}