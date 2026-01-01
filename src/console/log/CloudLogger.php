<?php

namespace pocketcloud\cloud\console\log;

use InvalidArgumentException;
use pmmp\thread\Thread;
use pocketcloud\cloud\console\log\logger\ILogger;
use pocketcloud\cloud\console\log\logger\Logger;
use pocketcloud\cloud\console\log\logger\ThreadLogger;
use RuntimeException;
use const pocketcloud\LOG_PATH;

final class CloudLogger {

    private static ?ILogger $instance = null;

    public static function set(?ILogger $logger): void {
        if ($logger === null) {
            self::$instance = null;
            return;
        }

        if (Thread::getCurrentThread() !== null && !$logger instanceof ThreadLogger) {
            throw new InvalidArgumentException("You can't set a non-instance of ThreadLogger as your logger inside a thread");
        } else if (Thread::getCurrentThread() === null && $logger instanceof ThreadLogger) {
            throw new InvalidArgumentException("You can't set a instance of ThreadLogger as your logger outside a thread");
        }

        self::$instance = $logger;
    }
    
    public static function get(): ILogger {
        if (Thread::getCurrentThread() !== null) {
            if (self::$instance === null) throw new RuntimeException("No logger available for this thread");
            if (self::$instance instanceof ThreadLogger) return self::$instance;
            else throw new RuntimeException("Set logger for this thread is not an instance of ThreadLogger");
        } else if (self::$instance instanceof ThreadLogger) {
            throw new RuntimeException("Set logger for this thread is an instance of ThreadLogger, but you can't use the ThreadLogger outside a thread");
        }

        return self::$instance ??= new Logger(defined("pocketcloud\LOG_PATH") ? LOG_PATH : null, false, false);
    }

    public static function tmp(?string $cloudLogPath = null, bool $debugMode = false, bool $saveLogs = false): ILogger {
        if (Thread::getCurrentThread() !== null) throw new RuntimeException("You cannot create a temporary logger inside a thread");
        return new Logger($cloudLogPath, $debugMode, $saveLogs);
    }
}