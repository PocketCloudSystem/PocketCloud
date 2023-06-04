<?php

namespace pocketcloud\util;

class ExceptionHandler {

    public static function handle(\Throwable $throwable) {
        $send = true;
        if ($throwable instanceof \ErrorException) if ((error_reporting() & $throwable->getSeverity()) == 0) $send = false;
        if ($send) CloudLogger::get()->exception($throwable);
    }

    public static function set() {
        set_error_handler(fn(int $errno, string $error, string $file, int $line) => self::handle(new \ErrorException($error, 0, $errno, $file, $line)));
        set_exception_handler([self::class, "handle"]);
    }
}