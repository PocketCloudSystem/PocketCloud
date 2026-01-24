<?php

namespace pocketcloud\cloud\util;

final class TerminalUtils {

    private static string $pendingBuffer = "";

    public static function invertColors(string $text): string {
        return "\033[7m" . $text . "\033[27m";
    }

    public static function getCurrentUser(): string {
        return match (PHP_OS_FAMILY) {
            "Windows" => getenv("USERNAME"),
            default => getenv("USER")
        };
    }

    public static function buffer(string $text): void {
        self::$pendingBuffer .= $text;
    }

    public static function flush(): void {
        self::write(self::$pendingBuffer);
        self::$pendingBuffer = "";
    }

    public static function write(string $text): void {
        echo $text;
    }

    public static function rewindCursor(bool $buffer = false): void {
        if ($buffer) self::buffer("\r");
        else self::write("\r");
    }

    public static function clearLine(bool $rewindCursor = false, bool $buffer = false): void {
        if ($buffer) self::buffer("\033[2K");
        else self::write("\033[2K");
        if ($rewindCursor) self::rewindCursor($buffer);
    }

    public static function moveCursorUp(int $lines = 1, bool $buffer = false): void {
        if ($buffer) self::buffer("\033[{$lines}A");
        else self::write("\033[{$lines}A");
    }

    public static function moveCursorDown(int $lines = 1, bool $buffer = false): void {
        if ($buffer) self::buffer("\033[{$lines}B");
        else self::write("\033[{$lines}B");
    }

    public static function moveCursorLeft(int $columns = 1, bool $buffer = false): void {
        if ($buffer) self::buffer("\033[{$columns}D");
        else self::write("\033[{$columns}D");
    }

    public static function moveCursorRight(int $columns = 1, bool $buffer = false): void {
        if ($buffer) self::buffer("\033[{$columns}C");
        else self::write("\033[{$columns}C");
    }

    public static function setCursorPosition(int $column, bool $buffer = false): void {
        self::rewindCursor($buffer);
        self::moveCursorRight($column, $buffer);
    }

    public static function hideCursor(bool $buffer = false): void {
        if ($buffer) self::buffer("\033[?25l");
        else self::write("\033[?25l");
    }

    public static function showCursor(bool $buffer = false): void {
        if ($buffer) self::buffer("\033[?25h");
        else self::write("\033[?25h");
    }

    public static function clearPrompt(bool $buffer = false): void {
        self::clearLine(true, $buffer);
    }

    public static function clearConsole(): void {
        echo chr(27) . chr(91) . "H" . chr(27) . chr(91) . "J";
    }
    
    public static function checkCommand(string $command): bool {
        if (PHP_OS_FAMILY == "Linux") {
            $output = shell_exec(sprintf("which %s", escapeshellarg($command)));
            return $output !== null && $output !== false;
        }
        return false;
    }
}