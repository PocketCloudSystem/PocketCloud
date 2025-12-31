<?php

namespace pocketcloud\cloud\util\trait;

trait RegistryTrait {

    /** @ignored */
    protected static ?array $members = null;

    final protected static function register(string $name, mixed $member): void {
        if (self::$members !== null) {
            self::$members[strtoupper($name)] = $member;
        }
    }

    final public static function getAll(): array {
        self::check();
        return self::$members;
    }

    final public static function get(string $name): mixed {
        self::check();
        return self::$members[strtoupper($name)] ?? null;
    }

    protected static function check(): void {
        if (self::$members === null) {
            self::$members = [];
            static::init();
        }
    }

    protected static function init(): void {}

    public static function __callStatic(string $name, array $arguments) {
        self::check();
        if (isset(self::$members[strtoupper($name)])) {
            if (is_callable(self::$members[strtoupper($name)])) {
                return (self::$members[strtoupper($name)])(...$arguments);
            } else {
                return self::$members[strtoupper($name)];
            }
        }
        return null;
    }
}