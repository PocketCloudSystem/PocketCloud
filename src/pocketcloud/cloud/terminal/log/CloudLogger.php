<?php

namespace pocketcloud\cloud\terminal\log;

use pocketcloud\cloud\terminal\log\logger\Logger;

final class CloudLogger {

    private static ?Logger $logger = null;

    public static function set(?Logger $logger): void {
        self::$logger = $logger;
    }

    public static function get(): Logger {
        if (self::$logger === null) self::set(new Logger(LOG_PATH));
        return self::$logger;
    }

    public static function close(): void {
        self::$logger?->close();
    }
}