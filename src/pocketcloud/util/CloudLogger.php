<?php

namespace pocketcloud\util;

use pocketcloud\config\DefaultConfig;
use pocketcloud\console\log\Logger;

final class CloudLogger {

    private static ?Logger $logger = null;

    public static function set(?Logger $logger) {
        self::$logger = $logger;
    }

    public static function get(): Logger {
        try {
            if (self::$logger === null) self::set(new Logger(LOG_PATH, DefaultConfig::getInstance()->isDebugMode()));
        } catch (\Throwable $exception) {
            self::set(new Logger(LOG_PATH, true));
        }
        return self::$logger;
    }

    public static function close() {
        if (self::$logger !== null) self::$logger->close();
    }
}