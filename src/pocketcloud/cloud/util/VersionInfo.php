<?php

namespace pocketcloud\cloud\util;

final class VersionInfo {

    public const string VERSION = "3.1.1";
    public const array DEVELOPERS = ["r3pt1s"];
    public const bool BETA = false;

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