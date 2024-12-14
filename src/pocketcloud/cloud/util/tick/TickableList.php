<?php

namespace pocketcloud\cloud\util\tick;

use pocketcloud\cloud\update\UpdateChecker;

final class TickableList {

    /** @var array<Tickable> */
    private static array $list = [];

    public static function add(Tickable $tickable): void {
        self::$list[spl_object_id($tickable)] = $tickable;
    }

    public static function remove(Tickable $tickable): void {
        if (isset(self::$list[spl_object_id($tickable)])) unset(self::$list[spl_object_id($tickable)]);
    }

    public static function tick(int $currentTick): void {
        if (UpdateChecker::getInstance()->isUpdating()) return;
        foreach (self::$list as $tickable) {
            $tickable->tick($currentTick);
        }
    }

    public static function getAll(): array {
        return self::$list;
    }
}