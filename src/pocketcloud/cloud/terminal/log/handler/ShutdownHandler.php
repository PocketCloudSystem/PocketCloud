<?php

namespace pocketcloud\cloud\terminal\log\handler;

use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\terminal\log\CloudLogger;

final class ShutdownHandler {

    public static function register(): void {
        register_shutdown_function(fn() => self::shutdown());

        if (function_exists("pcntl_signal")) {
            pcntl_signal(SIGTERM, fn(int $signo) => self::shutdown());
            pcntl_signal(SIGINT, fn(int $signo) => self::shutdown());
            pcntl_signal(SIGHUP, fn(int $signo) => self::shutdown());
            pcntl_async_signals(true);
        }
    }

    public static function unregister(): void {
        register_shutdown_function(fn() => null);

        if (function_exists("pcntl_signal")) {
            pcntl_signal(SIGTERM, SIG_DFL);
            pcntl_signal(SIGINT, SIG_DFL);
            pcntl_signal(SIGHUP, SIG_DFL);
        }
    }

    private static function shutdown(): void {
        CloudLogger::get()->emptyLine();
        PocketCloud::getInstance()->shutdown();
    }
}