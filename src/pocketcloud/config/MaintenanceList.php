<?php

namespace pocketcloud\config;

use pocketcloud\util\Config;
use pocketcloud\util\Reloadable;

class MaintenanceList implements Reloadable {

    private static ?Config $config = null;

    private static function check(): void {
        if (self::$config === null) self::$config = new Config(IN_GAME_PATH . "maintenanceList.json", 1);
    }

    public static function add(string $playerName): void {
        self::check();
        self::$config->set($playerName, true);
        self::$config->save();
    }

    public static function remove(string $playerName): void {
        self::check();
        self::$config->remove($playerName);
        self::$config->save();
    }

    public static function is(string $playerName): bool {
        self::check();
        return self::$config->exists($playerName) && self::$config->get($playerName, false);
    }

    public static function all(): array {
        self::check();
        return array_filter(self::$config->getAll(true), fn(string $name) => self::$config->get($name));
    }

    public function reload(): bool {
        self::check();
        self::$config->reload();
        return true;
    }
}