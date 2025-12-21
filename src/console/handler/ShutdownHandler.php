<?php

namespace pocketcloud\cloud\console\handler;

use pocketcloud\cloud\PocketCloud;

final class ShutdownHandler {

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
        PocketCloud::getInstance()->shutdown();
    }

    private static function handleCrash(): void {
        PocketCloud::getInstance()->crash();
    }
}