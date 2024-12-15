<?php

namespace pocketcloud\cloud\terminal\log\level;

use pocketcloud\cloud\util\enum\EnumTrait;

/**
 * @method static CloudLogLevel INFO()
 * @method static CloudLogLevel WARN()
 * @method static CloudLogLevel ERROR()
 * @method static CloudLogLevel DEBUG()
 * @method static CloudLogLevel SUCCESS()
 */
final class CloudLogLevel {
    use EnumTrait;

    protected static function init(): void {
        self::register("info", new CloudLogLevel("INFO", "§bINFO"));
        self::register("warn", new CloudLogLevel("WARN", "§cWARN"));
        self::register("error", new CloudLogLevel("ERROR", "§4ERROR"));
        self::register("success", new CloudLogLevel("SUCCESS", "§aSUCCESS"));
        self::register("debug", new CloudLogLevel("DEBUG", "§6DEBUG"));
    }

    public function __construct(
        private readonly string $name,
        private readonly string $prefix
    ) {}

    public function getName(): string {
        return $this->name;
    }

    public function getPrefix(): string {
        return $this->prefix;
    }
}