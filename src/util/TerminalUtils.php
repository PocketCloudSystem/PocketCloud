<?php

namespace pocketcloud\cloud\util;

final class TerminalUtils {

    public static function getCurrentUser(): string {
        return match (PHP_OS_FAMILY) {
            "Windows" => getenv("USERNAME"),
            default => getenv("USER")
        };
    }

    public static function clearPrompt(): void {
        echo "\033[2K\r";
    }

    public static function clear(): void {
        echo chr(27) . chr(91) . "H" . chr(27) . chr(91) . "J";
    }

    public static function kill(int $pid, bool $subprocesses = true): void {
        if ($subprocesses) $pid = -$pid;

        if (function_exists("posix_kill")) {
            posix_kill($pid, 9);
        } else {
            exec("kill -9 $pid > /dev/null 2>&1");
        }
    }

    public static function checkCommand(string $command): bool {
        if (PHP_OS_FAMILY == "Linux") {
            $output = shell_exec(sprintf("which %s", escapeshellarg($command)));
            return $output !== null && $output !== false;
        }
        return false;
    }
}