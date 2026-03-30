<?php

namespace pocketcloud\cloud\console\handler;

use ErrorException;
use pocketcloud\cloud\PocketCloud;

final class ShutdownHandler {

    private static bool $handling = false;

    public static function register(): void {
        register_shutdown_function(self::handleCrash(...));

        if (function_exists("pcntl_signal")) {
            pcntl_signal(SIGTERM, self::handleShutdown(...));
            pcntl_signal(SIGINT, self::handleShutdown(...));
            pcntl_signal(SIGHUP, self::handleShutdown(...));
            pcntl_async_signals(true);
        }
    }

    public static function remove(): void {
        if (function_exists("pcntl_signal")) {
            pcntl_signal(SIGTERM, SIG_DFL);
            pcntl_signal(SIGINT, SIG_DFL);
            pcntl_signal(SIGHUP, SIG_DFL);
        }
    }

    private static function handleShutdown(): void {
        if (self::$handling) return;
        self::$handling = true;
        self::remove();
        PocketCloud::getInstance()->shutdown();
    }

    /**
     * @throws ErrorException
     */
    private static function handleCrash(): void {
        if (self::$handling) return;
        self::$handling = true;
        self::remove();

        $error = error_get_last();
        if ($error !== null && in_array($error["type"], [E_ERROR, E_CORE_ERROR, E_COMPILE_ERROR, E_PARSE])) {
            ExceptionHandler::handleError($error["type"], $error["message"], $error["file"], $error["line"]);
            PocketCloud::getInstance()->crash();
        } else {
            PocketCloud::getInstance()->shutdown();
        }
    }
}