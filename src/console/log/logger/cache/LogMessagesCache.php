<?php

namespace pocketcloud\cloud\console\log\logger\cache;

use pocketcloud\cloud\console\log\CloudLogger;

final class LogMessagesCache {

    public const int MAX_LINES_IN_MEMORY = 100;

    private static array $savedLines = [];

    public static function save(string $line): void {
        self::$savedLines[] = $line;
        if (count(self::$savedLines) > self::MAX_LINES_IN_MEMORY) {
            self::$savedLines = array_slice(self::$savedLines, -self::MAX_LINES_IN_MEMORY);
        }
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