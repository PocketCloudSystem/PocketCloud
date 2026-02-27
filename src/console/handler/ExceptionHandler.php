<?php

namespace pocketcloud\cloud\console\handler;

use Closure;
use ErrorException;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\output\OutputManager;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\ErrorUtils;
use Throwable;

final class ExceptionHandler {

    private static ?Throwable $latestException = null;
    private static int $errorLevels = -1;

    public static function handleException(Throwable $throwable): void {
        self::$latestException = $throwable;
        OutputManager::reset();
        CloudLogger::get()->exception($throwable);
        PocketCloud::getInstance()->crash();
    }

    /**
     * @throws ErrorException
     */
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

    public static function throwAll(): Closure {
        return function (int $errno, string $error, string $file, int $line) {
            throw new ErrorException($error, 0, $errno, $file, $line);
        };
    }

    /**
     * For recoverable operations. Logs the error and returns null, never crashes.
     */
    public static function attempt(Closure $closure, ?string $message = null, ?Closure $onExceptionClosure = null, mixed ...$params): mixed {
        set_error_handler(self::throwAll(...));
        try {
            return $closure(...$params);
        } catch (Throwable $e) {
            if ($message !== null) CloudLogger::get()->error($message . ": " . $e->getMessage());
            if ($onExceptionClosure !== null) ($onExceptionClosure)($exception);
            return null;
        } finally {
            restore_error_handler();
        }
    }

    /**
     * For truly fatal operations. Crashes the cloud on failure.
     */
    public static function require(Closure $closure, ?string $message = null, ?Closure $onExceptionClosure = null, mixed ...$params): mixed {
        set_error_handler(self::throwAll(...));
        try {
            return $closure(...$params);
        } catch (Throwable $e) {
            if ($message !== null) CloudLogger::get()->error($message);
            if ($onExceptionClosure !== null) ($onExceptionClosure)($exception);
            self::handleException($e);
            return null;
        } finally {
            restore_error_handler();
        }
    }

    public static function latestException(): ?Throwable {
        return self::$latestException;
    }
}