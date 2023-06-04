<?php

namespace pocketcloud\util;

class TickableList {

    /** @var array<Tickable> */
    private static array $list = [];

    public static function add(Tickable $tickable) {
        self::$list[spl_object_id($tickable)] = $tickable;
    }

    public static function remove(Tickable $tickable) {
        if (isset(self::$list[spl_object_id($tickable)])) unset(self::$list[spl_object_id($tickable)]);
    }

    public static function tick(int $currentTick) {
        foreach (self::$list as $tickable) {
            $tickable->tick($currentTick);
        }
    }

    public static function getList(): array {
        return self::$list;
    }
}