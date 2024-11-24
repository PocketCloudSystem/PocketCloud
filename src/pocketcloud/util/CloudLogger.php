<?php

namespace pocketcloud\util;

use pocketcloud\config\impl\DefaultConfig;
use pocketcloud\console\log\Logger;
use Throwable;

final class CloudLogger {

    private static ?Logger $logger = null;

    public static function set(?Logger $logger): void {
        self::$logger = $logger;
    }

    public static function get(): Logger {
        try {
            if (self::$logger === null) self::set(new Logger(LOG_PATH));
        } catch (Throwable) {
            self::set(new Logger(LOG_PATH, forceDebug: true));
        }
        return self::$logger;
    }

    public static function close(): void {
        self::$logger?->close();
    }
}