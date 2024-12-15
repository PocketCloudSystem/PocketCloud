<?php

namespace pocketcloud\cloud\util\terminal;

final class TerminalUtils {

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
}