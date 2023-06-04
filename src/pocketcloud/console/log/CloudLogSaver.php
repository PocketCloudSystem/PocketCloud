<?php

namespace pocketcloud\console\log;

class CloudLogSaver {

    private static array $savedLines = [];

    public static function save(string $line) {
        self::$savedLines[] = $line;
    }

    public static function clear() {
        self::$savedLines = [];
    }

    public static function print() {
        foreach (self::$savedLines as $line) {
            echo $line;
        }
    }
}