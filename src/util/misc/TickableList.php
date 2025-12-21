<?php

namespace pocketcloud\cloud\util\misc;

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
            $instance->tick($currentTick);
        }
    }
}