<?php

namespace pocketcloud\cloud\util\enum;

trait EnumTrait {

    /** @ignored */
    protected static ?array $members = null;

    final public static function register(string $name, mixed $member): void {
        if (self::$members !== null) {
            self::$members[strtoupper($name)] = $member;
        }
    }

    final public static function getAll(): array {
        self::check();
        return self::$members;
    }

    private static function check(): void {
        if (self::$members === null) {
            self::$members = [];
            static::init();
        }
    }

    protected static function init(): void {}

    public static function __callStatic(string $name, array $arguments) {
        self::check();
        if (isset(self::$members[strtoupper($name)])) return self::$members[strtoupper($name)];
        return null;
    }
}