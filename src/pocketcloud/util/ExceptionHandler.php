<?php

namespace pocketcloud\util;

use Closure;
use ErrorException;
use Throwable;

class ExceptionHandler {

    public static function handle(Throwable $throwable): void {
        $send = true;
        if ($throwable instanceof ErrorException) if ((error_reporting() & $throwable->getSeverity()) == 0) $send = false;
        if ($send) CloudLogger::get()->exception($throwable);
    }

    public static function set(): void {
        set_error_handler(fn(int $errno, string $error, string $file, int $line) => self::handle(new ErrorException($error, 0, $errno, $file, $line)));
        set_exception_handler([self::class, "handle"]);
    }

    public static function tryCatch(Closure $processClosure, ?string $message = null, ?Closure $onExceptionClosure = null): mixed {
        try {
            return ($processClosure)();
        } catch (Throwable $exception) {
            if ($message !== null) CloudLogger::get()->error($message);
            self::handle($exception);
            ($onExceptionClosure)($exception);
        }

        return null;
    }
}