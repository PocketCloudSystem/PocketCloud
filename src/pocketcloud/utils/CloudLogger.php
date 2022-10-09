<?php

namespace pocketcloud\utils;

use pocketcloud\config\CloudConfig;
use pocketcloud\console\log\Logger;

final class CloudLogger {

    private static ?Logger $logger = null;

    public static function set(?Logger $logger) {
        self::$logger = $logger;
    }

    public static function get(): Logger {
        if (self::$logger === null) self::set(new Logger(LOG_PATH, CloudConfig::getInstance()->isDebugMode()));
        return self::$logger;
    }
}