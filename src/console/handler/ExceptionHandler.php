<?php

namespace pocketcloud\cloud\console\handler;

use Closure;
use ErrorException;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\ErrorUtils;
use Throwable;

final class ExceptionHandler {

    private static ?Throwable $latestException = null;
    private static int $errorLevels = -1;

    public static function handleException(Throwable $throwable): void {
        self::$latestException = $throwable;
        CloudLogger::get()->exception($throwable);
        PocketCloud::getInstance()->crash();
    }

    public static function handleError(int $errno, string $error, string $file, int $line): bool {
        if (!(error_reporting() & $errno)) return false;

        if (self::$errorLevels & $errno) {
            throw new ErrorException($error, 0, $errno, $file, $line);
        }

        CloudLogger::get()->warn("§e{}§c: §e{} §cin §e{} §cat line §e{}", ErrorUtils::getTypeName($errno), $error, $file, $line);
        return true;
    }

    public static function setAll(int $levels = E_NOTICE | E_WARNING): void {
        self::setErrorHandler($levels);
        self::setExceptionHandler();
    }

    public static function setErrorHandler(int $levels = E_NOTICE | E_WARNING): void {
        self::$errorLevels = $levels;
        set_error_handler(self::handleError(...));
    }

    public static function setExceptionHandler(): void {
        set_exception_handler(self::handleException(...));
    }

    public static function tryCatch(Closure $processClosure, ?string $message = null, ?Closure $onExceptionClosure = null, mixed ...$params): mixed {
        set_error_handler(function (int $errno, string $error, string $file, int $line) {
            if (!(error_reporting() & $errno)) {
                return false;
            }

            throw new ErrorException($error, 0, $errno, $file, $line);
        });

        try {
            return $processClosure(...$params);
        } catch (Throwable $exception) {
            if ($message !== null) CloudLogger::get()->error($message);
            self::handleException($exception);
            if ($onExceptionClosure !== null) ($onExceptionClosure)($exception);
        } finally {
            restore_error_handler();
        }

        return null;
    }

    public static function latestException(): ?Throwable {
        return self::$latestException;
    }
}