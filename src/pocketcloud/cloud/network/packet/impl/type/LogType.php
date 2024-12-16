<?php

namespace pocketcloud\cloud\network\packet\impl\type;

use pocketcloud\cloud\util\enum\EnumTrait;

/**
 * @method static LogType INFO()
 * @method static LogType DEBUG()
 * @method static LogType WARN()
 * @method static LogType ERROR()
 * @method static LogType SUCCESS()
 */
final class LogType {
    use EnumTrait;

    protected static function init(): void {
        self::register("info", new LogType("INFO"));
        self::register("debug", new LogType("DEBUG"));
        self::register("warn", new LogType("WARN"));
        self::register("error", new LogType("ERROR"));
        self::register("success", new LogType("SUCCESS"));
    }

    public function __construct(private readonly string $name) {}

    public function getName(): string {
        return $this->name;
    }
}