<?php

namespace pocketcloud\cloud\util\enum;

use Closure;

trait ParameterEnumTrait {

    /** @ignored */
    protected static ?array $members = null;

    final public static function register(string $name, Closure $onCreate): void {
        if (self::$members !== null) {
            self::$members[strtoupper($name)] = $onCreate;
        }
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
        if (isset(self::$members[strtoupper($name)])) return (self::$members[strtoupper($name)])(...$arguments);
        return null;
    }
}