<?php

namespace pocketcloud\console\log\level;

use pocketcloud\util\EnumTrait;

/**
 * @method static CloudLogLevel INFO()
 * @method static CloudLogLevel WARN()
 * @method static CloudLogLevel ERROR()
 * @method static CloudLogLevel DEBUG()
 */
final class CloudLogLevel {
    use EnumTrait;

    protected static function init(): void {
        self::register("info", new CloudLogLevel("INFO", "§bINFO"));
        self::register("warn", new CloudLogLevel("WARN", "§cWARN"));
        self::register("error", new CloudLogLevel("ERROR", "§4ERROR"));
        self::register("debug", new CloudLogLevel("debug", "§6DEBUG"));
    }

    public function __construct(private string $name, private string $prefix) {}

    public function getName(): string {
        return $this->name;
    }

    public function getPrefix(): string {
        return $this->prefix;
    }
}