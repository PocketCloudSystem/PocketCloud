<?php

namespace pocketcloud\network\packet\impl\types;

use pocketcloud\util\EnumTrait;

/**
 * @method static LogType INFO()
 * @method static LogType DEBUG()
 * @method static LogType WARN()
 * @method static LogType ERROR()
 */
final class LogType {
    use EnumTrait;

    protected static function init(): void {
        self::register("info", new LogType("INFO"));
        self::register("debug", new LogType("DEBUG"));
        self::register("warn", new LogType("WARN"));
        self::register("error", new LogType("ERROR"));
    }

    public static function getTypeByName(string $name): ?LogType {
        self::check();
        return self::$members[strtoupper($name)] ?? null;
    }

    public function __construct(private readonly string $name) {}

    public function getName(): string {
        return $this->name;
    }
}