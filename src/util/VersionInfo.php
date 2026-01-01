<?php

namespace pocketcloud\cloud\util;

final class VersionInfo {

    public const string VERSION = "4.0.0";
    public const array DEVELOPERS = ["r3pt1s"];
    public const bool BETA = true;
    public const int METRICS_ID = 28627;

    public static function getVersion(): int {
        return self::VERSION;
    }

    public static function getDevelopers(): array {
        return self::DEVELOPERS;
    }

    public static function isBeta(): bool {
        return self::BETA;
    }

    public static function getMetricsId(): int {
        return self::METRICS_ID;
    }
}