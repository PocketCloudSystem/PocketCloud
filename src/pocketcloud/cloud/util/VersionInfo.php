<?php

namespace pocketcloud\cloud\util;

final class VersionInfo {

    public const VERSION = "3.0.0";
    public const DEVELOPERS = ["r3pt1s"];
    public const BETA = true;

    public static function getVersion(): int {
        return self::VERSION;
    }

    public static function getDevelopers(): array {
        return self::DEVELOPERS;
    }

    public static function isBeta(): bool {
        return self::BETA;
    }
}