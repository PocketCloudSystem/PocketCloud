<?php

namespace pocketcloud\cloud\util\misc;

final class LoadableList {

    /** @var array<Loadable> */
    private static array $loadableList = [];
    private static bool $loaded = false;

    public static function add(Loadable ...$instances): void {
        if (self::$loaded) return;
        foreach ($instances as $instance) self::$loadableList[] = $instance;
    }

    public static function clear(): void {
        self::$loadableList = [];
    }

    public static function loadAll(): void {
        if (self::$loaded) return;
        self::$loaded = true;
        foreach (self::$loadableList as $instance) {
            $instance->load();
        }

        self::clear();
    }
}