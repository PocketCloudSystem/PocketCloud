<?php

namespace pocketcloud\cloud\console\handler;

use Closure;
use ErrorException;
use pocketcloud\cloud\console\log\CloudLogger;
use Throwable;

final class ExceptionHandler {

    public static function handleException(Throwable $throwable): void {
        CloudLogger::get()->exception($throwable);
    }

    public static function handleError(int $errno, string $error, string $file, int $line): void {
        if ((error_reporting() & $errno) !== 0) {
            throw new ErrorException($error, 0, $errno, $file, $line);
        }
    }

    public static function setAll(): void {
        self::setErrorHandler();
        self::setExceptionHandler();
    }

    public static function setErrorHandler(): void {
        set_error_handler(self::handleError(...));
    }

    public static function setExceptionHandler(): void {
        set_exception_handler(self::handleException(...));
    }

    public static function tryCatch(Closure $processClosure, ?string $message = null, ?Closure $onExceptionClosure = null, mixed ...$params): mixed {
        set_error_handler(fn(int $errno, string $error, string $file, int $line) => new ErrorException($error, 0, $errno, $file, $line));
        try {
            return $processClosure(...$params);
        } catch (Throwable $exception) {
            if ($message !== null) CloudLogger::get()->error($message);
            self::handleException($exception);
            ($onExceptionClosure)($exception);
        } finally {
            restore_error_handler();
        }

        return null;
    }
}