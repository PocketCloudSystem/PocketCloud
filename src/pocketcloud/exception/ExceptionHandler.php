<?php

namespace pocketcloud\exception;

use pocketcloud\utils\CloudLogger;

class ExceptionHandler {

    public static function handle(\Throwable $throwable) {
        CloudLogger::get()->exception($throwable);
    }

    public static function set() {
        set_error_handler(function(int $errno, string $error, string $file, int $line) {
            throw new \ErrorException($error, 0, $errno, $file, $line);
        });

        set_exception_handler([self::class, "handle"]);
    }
}