<?php

namespace pocketcloud\cloud\console;

use Closure;
use Exception;
use pocketcloud\cloud\util\TerminalUtils;

/** @author ChatGPT + Gemini (thanks bro) */
final class ManualConsole {

    private bool $closed = false;
    
    private array $history = [];
    private int $historyIndex = 0;
    private int $cursor = 0;
    private string $input = "";

    private array $tabMatches = [];
    private int $tabIndex = 0;
    private bool $tabActive = false;

    public function __construct(
        private string $prompt = "",
        private ?Closure $completionCallback = null,
        private ?Closure $controlCHandler = null
    ) {
        stream_set_blocking(STDIN, false);
        mb_internal_encoding("UTF-8");
        shell_exec("stty -echo");
        shell_exec("stty raw");
    }

    public function readlineNonBlocking(int $timeoutMs = 0): ?string {
        $this->ensureOpen();
        $this->redraw($this->prompt);

        $char = $this->readChar($timeoutMs);

        if ($char === null) return null;

        if ($char !== "\t") $this->tabActive = false;

        if ($char === "\x03") {
            if ($this->controlCHandler !== null) ($this->controlCHandler)();
            return null;
        }

        if ($char === "\n" || $char === "\r") {
            echo "\n";
            if ($this->input !== "") $this->history[] = $this->input;
            $this->historyIndex = count($this->history);
            $line = $this->input;
            $this->input = "";
            $this->cursor = 0;
            return $line;
        }

        if ($char === "\033") {
            $seq = $this->readChar(0) . $this->readChar(0);
            $this->handleEscapeSequence($seq, $this->prompt);
            return null;
        }

        if ($char === "\t") {
            $this->handleTabCompletion($this->prompt);
            return null;
        }

        if ($char === "\177") {
            if ($this->cursor > 0) {
                $before = mb_substr($this->input, 0, $this->cursor - 1);
                $after = mb_substr($this->input, $this->cursor);
                $this->input = $before . $after;
                $this->cursor--;
                $this->redraw($this->prompt);
            }
            return null;
        }

        $this->input = mb_substr($this->input, 0, $this->cursor) . $char . mb_substr($this->input, $this->cursor);
        $this->cursor++;
        $this->redraw($this->prompt);

        return null;
    }

    private function handleTabCompletion(string $prompt): void {
        if ($this->completionCallback === null) return;

        $beforeCursor = substr($this->input, 0, $this->cursor);
        $afterCursor = substr($this->input, $this->cursor);

        $tokens = $this->tokenize($beforeCursor);
        $current = array_pop($tokens) ?? "";

        if (!$this->tabActive) {
            $this->tabMatches = array_values(array_map(fn($match) => str_contains($match, " ") ? '"' . $match . '"' : $match, ($this->completionCallback)($tokens, $current)));
            $this->tabIndex = 0;
            $this->tabActive = true;

            if (empty($this->tabMatches)) {
                $this->tabActive = false;
                return;
            }

            $common = $this->longestCommonPrefix($this->tabMatches);
            if (strlen($common) > strlen($current)) {
                $tokens[] = $common;
                $this->input = implode(" ", $tokens) . $afterCursor;
                $this->cursor = strlen(implode(" ", $tokens));
                $this->redraw($prompt);
                return;
            }

            echo "\n\r" . implode(" ", $this->tabMatches) . "\n";
            $this->redraw($prompt);
            return;
        }

        $match = $this->tabMatches[$this->tabIndex];
        $this->tabIndex = ($this->tabIndex + 1) % count($this->tabMatches);
        $tokens[] = $match;
        $this->input = implode(" ", $tokens) . $afterCursor;
        $this->cursor = strlen(implode(" ", $tokens));
        $this->redraw($prompt);
    }

    private function tokenize(string $line): array {
        $actualParts = [];
        $parts = explode(" ", $line);
        $currentQuotationStartIndex = 0;
        $currentQuotationPart = "";
        $inQuotation = false;
        foreach ($parts as $i => $part) {
            if (str_starts_with($part, '"') || str_starts_with($part, "'")) {
                $inQuotation = true;
                $currentQuotationStartIndex = $i;
                $currentQuotationPart .= $part;
            } else {
                if ($inQuotation) {
                    $currentQuotationPart .= " " . $part;
                    if (str_ends_with($part, '"') || str_ends_with($part, "'")) {
                        $inQuotation = false;
                        $actualParts[$currentQuotationStartIndex] = $currentQuotationPart;
                    }
                } else $actualParts[$i] = $part;
            }
        }

        return $actualParts;
    }

    private function longestCommonPrefix(array $strings): string {
        if (empty($strings)) return "";

        $prefix = $strings[0];

        foreach ($strings as $str) {
            $i = 0;
            $max = min(strlen($prefix), strlen($str));
            while ($i < $max && $prefix[$i] === $str[$i]) $i++;
            $prefix = substr($prefix, 0, $i);
            if ($prefix === "") break;
        }

        return $prefix;
    }

    private function readChar(int $timeoutMs): ?string {
        $this->ensureOpen();

        $read = [STDIN];
        $write = null;
        $except = null;
        $tv_usec = ($timeoutMs % 1000) * 1000;

        if (stream_select($read, $write, $except, 0, $tv_usec) <= 0) return null;

        $char = fread(STDIN, 1);
        if ($char === false || $char === "") return null;

        $ord = ord($char);

        if ($ord < 0x80) {
            return $char;
        }

        $bytes = [$char];

        if (($ord & 0xE0) === 0xC0) {
            $length = 2;
        } elseif (($ord & 0xF0) === 0xE0) {
            $length = 3;
        } elseif (($ord & 0xF8) === 0xF0) {
            $length = 4;
        } else {
            return null;
        }

        for ($i = 1; $i < $length; $i++) {
            $next = fread(STDIN, 1);
            if ($next === false) return null;
            $bytes[] = $next;
        }

        return implode("", $bytes);
    }

    private function handleEscapeSequence(string $seq, string $prompt): void {
        $this->ensureOpen();
        switch ($seq) {
            case "[A": // Up
                if ($this->historyIndex > 0) {
                    $this->historyIndex--;
                    $this->input = $this->history[$this->historyIndex];
                    $this->cursor = strlen($this->input);
                    $this->redraw($prompt);
                }
                break;
            case "[B": // Down
                if ($this->historyIndex < count($this->history) - 1) {
                    $this->historyIndex++;
                    $this->input = $this->history[$this->historyIndex];
                } else {
                    $this->input = "";
                    $this->historyIndex = count($this->history);
                }
                $this->cursor = strlen($this->input);
                $this->redraw($prompt);
                break;
            case "[C": // Right
                if ($this->cursor < strlen($this->input)) $this->cursor++;
                break;
            case "[D": // Left
                if ($this->cursor > 0) $this->cursor--;
                break;
        }
    }

    private function redraw(string $prompt): void {
        echo "\033[2K\r";
        echo $prompt . $this->input;
        $back = mb_strlen($this->input) - $this->cursor;
        if ($back > 0) echo str_repeat("\033[D", $back);
    }

    public function println(string $message): void {
        echo "\033[2K\r";
        echo $message . PHP_EOL;
        $this->redraw($this->prompt);
    }

    public function dump(mixed ...$vars): void {
        ob_start();
        var_dump(...$vars);
        $out = ob_get_clean();
        $out = str_replace("\t", "    ", $out);

        echo "\033[2K\r";
        echo rtrim($out) . PHP_EOL;
        $this->redraw($this->prompt);
    }

    public function ensureOpen(): void {
        if ($this->closed) throw new Exception("Console is already closed");
    }

    public function close(): void {
        $this->ensureOpen();
        shell_exec("stty echo");
        shell_exec("stty cooked");
        $this->closed = true;
        $this->history = [];
        $this->historyIndex = 0;
        $this->prompt = "";
        $this->input = "";
        TerminalUtils::clearPrompt();
        stream_set_blocking(STDIN, true);
    }

    public function __destruct() {
        shell_exec("stty echo");
        shell_exec("stty cooked");
    }

    public function setPrompt(string $prompt): void {
        $this->prompt = $prompt;
    }

    public function setCompletionCallback(Closure $callback): void {
        $this->completionCallback = $callback;
    }
}