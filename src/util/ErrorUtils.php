<?php

namespace pocketcloud\cloud\util;

use pocketcloud\cloud\console\handler\ExceptionHandler;

final class ErrorUtils {

    private const array ERROR_TYPES = [
        E_ERROR => "E_ERROR",
        E_WARNING => "E_WARNING",
        E_PARSE => "E_PARSE",
        E_NOTICE => "E_NOTICE",
        E_CORE_ERROR => "E_CORE_ERROR",
        E_CORE_WARNING => "E_CORE_WARNING",
        E_COMPILE_ERROR => "E_COMPILE_ERROR",
        E_COMPILE_WARNING => "E_COMPILE_WARNING",
        E_USER_ERROR => "E_USER_ERROR",
        E_USER_WARNING => "E_USER_WARNING",
        E_USER_NOTICE => "E_USER_NOTICE",
        E_RECOVERABLE_ERROR => "E_RECOVERABLE_ERROR",
        E_DEPRECATED => "E_DEPRECATED",
        E_USER_DEPRECATED => "E_USER_DEPRECATED"
    ];

    public static function latestError(int $skips = 0): ?array {
        $error = error_get_last();
        $latestException = ExceptionHandler::latestException();

        if ($latestException !== null) {
            return [
                "type" => $latestException::class,
                "message" => $latestException->getMessage(),
                "file" => $latestException->getFile(),
                "line" => $latestException->getLine(),
                "code" => $latestException->getCode(),
                "trace" => $latestException->getTrace()
            ];
        }

        if ($error === null) return null;

        return [
            "type" => self::getTypeName($error["type"]),
            "message" => $error["message"],
            "file" => $error["file"],
            "line" => $error["line"],
            "code" => 0,
            "trace" => self::getCurrentTrace($skips)
        ];
    }

    /** @author PMMP https://github.com/pmmp/PocketMine-MP/blob/50430762cf4a93a19a5621f9d0157e8009a8c15c/src/utils/Utils.php#L506 */
    public static function getCurrentTrace(int $skips = 0): array {
        $skips++;
        if (function_exists("xdebug_get_function_stack") && count($trace = @xdebug_get_function_stack()) !== 0) {
            $trace = array_reverse($trace);
        } else {
            $trace = new \Exception()->getTrace();
        }

        for ($i = 0; $i < $skips; $i++) {
            if (isset($trace[$i])) unset($trace[$i]);
        }
        return array_values($trace);
    }

    public static function getTypeName(int $type): string  {
        return self::ERROR_TYPES[$type] ?? "UNKNOWN";
    }

    public static function getType(string $name): ?int {
        if (in_array($name, self::ERROR_TYPES)) return array_search($name, self::ERROR_TYPES);
        return -1;
    }
}