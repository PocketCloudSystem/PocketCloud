<?php

namespace pocketcloud\cloud\console\log\logger\cache;

use pocketcloud\cloud\console\log\CloudLogger;

final class LogMessagesCache {

    private static array $savedLines = [];

    public static function save(string $line): void {
        self::$savedLines[] = $line;
    }

    public static function clear(): void {
        self::$savedLines = [];
    }

    public static function print(): void {
        foreach (self::$savedLines as $line) {
            CloudLogger::get()->echo($line);
        }
    }
}