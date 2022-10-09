<?php

namespace pocketcloud\utils;

trait EnumTrait {

    protected static ?array $members = null;

    protected static function register(string $name, mixed $member): void {
        if (self::$members !== null) {
            self::$members[strtoupper($name)] = $member;
        }
    }

    private static function check(): void {
        if (self::$members === null) {
            self::$members = [];
            static::init();
        }
    }

    protected static function init(): void {

    }

    public static function __callStatic(string $name, array $arguments) {
        self::check();
        if (isset(self::$members[strtoupper($name)])) return self::$members[strtoupper($name)];
        return null;
    }
}