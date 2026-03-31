<?php

namespace pocketcloud\cloud\console;

use Closure;
use pocketcloud\cloud\util\TerminalUtils;
use RuntimeException;

/** @author ChatGPT + Gemini + Claude (thanks Gs) */
final class ManualConsole {

    private bool $closed = false;
    private string $oldSttySettings;

    private bool $typingEnabled = true;
    private bool $visibleTyping = true;
    private bool $historyEnabled = true;
    private array $history = [];
    private int $historyIndex = 0;
    private bool $initialDrawDone = false;
    private int $cursor = 0;
    private string $input = "";
    private bool $pressedEnter = false;

    private array $tabMatches = [];
    private int $tabIndex = 0;
    private bool $tabActive = false;
    private bool $tabMatchesDisplayed = false;

    private ?string $pendingChar = null;

    // Cached terminal width — shell_exec("tput cols") on every keypress causes input delay
    private ?int $termWidth = null;

    public function __construct(
        private string $prompt = "",
        private ?Closure $completionCallback = null,
        private ?Closure $controlCHandler = null
    ) {
        stream_set_blocking(STDIN, false);
        mb_internal_encoding("UTF-8");
        $this->oldSttySettings = shell_exec("stty -g");
        shell_exec("stty -echo");
        shell_exec("stty raw");
        ini_set("display_errors", "0");
        ini_set("log_errors", "0");
    }

    public function setHistoryEnabled(bool $enabled): void {
        $this->historyEnabled = $enabled;
    }

    public function setTypingEnabled(bool $enabled): void {
        $this->typingEnabled = $enabled;
    }

    public function setVisibleTypingEnabled(bool $enabled): void {
        $this->visibleTyping = $enabled;
    }

    private function getTermWidth(): int {
        return $this->termWidth ??= max(1, (int)(trim(shell_exec("tput cols") ?: "80")));
    }

    private function matchesLineCount(): int {
        $raw = implode(" ", $this->tabMatches);
        return max(1, (int)ceil($this->visibleLength($raw) / $this->getTermWidth()));
    }

    public function readlineNonBlocking(int $timeoutMs = 0): ?string {
        if ($this->pressedEnter) {
            $this->pressedEnter = false;
            $this->initialDrawDone = false;
        }

        $this->ensureOpen();

        if (!$this->initialDrawDone) {
            $this->redraw($this->prompt);
            $this->initialDrawDone = true;
        }

        $char = $this->readChar($timeoutMs);
        if ($char === null) return null;

        if ($char !== "\t") {
            if ($this->tabActive) $this->clearTabDisplay();
            $this->tabActive = false;
        }

        if ($char === "\x03") {
            if ($this->controlCHandler !== null) ($this->controlCHandler)();
            return null;
        }

        if ($char === "\n" || $char === "\r") {
            if (trim($this->input) !== "") TerminalUtils::write("\n");
            if ($this->input !== "" && $this->historyEnabled) $this->history[] = $this->input;
            $this->historyIndex = count($this->history);
            $line = $this->input;
            $this->input = "";
            $this->cursor = 0;
            $this->pressedEnter = true;
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

        if ($this->typingEnabled) {
            $batch = $char;
            while (true) {
                $next = $this->readChar(0);
                if ($next === null) break;
                if (
                    $next === "\n" || $next === "\r" ||
                    $next === "\033" || $next === "\177" ||
                    $next === "\t"  || $next === "\x03"
                ) {
                    $this->pendingChar = $next;
                    break;
                }
                $batch .= $next;
            }

            $this->input = mb_substr($this->input, 0, $this->cursor) . $batch . mb_substr($this->input, $this->cursor);
            $this->cursor += mb_strlen($batch);
            $this->redraw($this->prompt);
        }

        return null;
    }

    private function handleTabCompletion(string $prompt): void {
        if ($this->completionCallback === null) return;
        if (!$this->typingEnabled) return;

        $beforeCursor = mb_substr($this->input, 0, $this->cursor);
        $afterCursor = mb_substr($this->input, $this->cursor);

        $tokens = array_values($this->tokenize($beforeCursor));
        $current = array_pop($tokens) ?? "";

        if (!$this->tabActive) {
            $matches = array_values(array_map(
                fn(string $match) => str_contains($match, " ") ? '"' . $match . '"' : $match,
                ($this->completionCallback)($tokens, $current)
            ));

            if ($current !== "") {
                $filtered = array_values(array_filter(
                    $matches,
                    fn(string $m) => str_starts_with(mb_strtolower($m), mb_strtolower($current))
                ));

                $this->tabMatches = !empty($filtered) ? $filtered : $matches;
            } else {
                $this->tabMatches = $matches;
            }

            $this->tabIndex = -1;
            $this->tabActive = true;

            if (empty($this->tabMatches)) {
                $this->tabActive = false;
                return;
            }

            $common = $this->longestCommonPrefix($this->tabMatches);
            if (mb_strlen($common) > mb_strlen($current)) {
                $tokens[] = $common;
                $this->input = implode(" ", $tokens) . $afterCursor;
                $this->cursor = mb_strlen(implode(" ", $tokens));
                $this->redraw($prompt);

                if (count($this->tabMatches) === 1) {
                    $this->tabActive = false;
                    return;
                }
                $this->displayTabMatches();
                return;
            } else {
                $this->tabIndex = 0;
                $match = $this->tabMatches[0];
                $tokens[] = $match;
                $this->input = implode(" ", $tokens) . $afterCursor;
                $this->cursor = mb_strlen(implode(" ", $tokens));
                $this->redraw($prompt);
                $this->displayTabMatches();
                return;
            }
        }

        $this->tabIndex = ($this->tabIndex + 1) % count($this->tabMatches);
        $match = $this->tabMatches[$this->tabIndex];
        $tokens[] = $match;
        $this->input = implode(" ", $tokens) . $afterCursor;
        $this->cursor = mb_strlen(implode(" ", $tokens));
        $this->redraw($prompt);
        $this->displayTabMatches();
    }

    private function displayTabMatches(): void {
        if (empty($this->tabMatches)) return;

        $this->clearTabDisplay();

        $display = [];
        foreach ($this->tabMatches as $i => $match) {
            $display[] = ($i === $this->tabIndex) ? TerminalUtils::invertColors($match) : $match;
        }

        TerminalUtils::write("\n\r" . implode(" ", $display));

        $linesUsed = $this->matchesLineCount();
        for ($i = 0; $i < $linesUsed; $i++) {
            TerminalUtils::moveCursorUp();
        }

        TerminalUtils::setCursorPosition($this->visibleLength($this->prompt) + $this->cursor);

        $this->tabMatchesDisplayed = true;
    }

    private function clearTabDisplay(): void {
        if (!$this->tabMatchesDisplayed) return;

        $linesUsed = $this->matchesLineCount();
        for ($i = 0; $i < $linesUsed; $i++) {
            TerminalUtils::moveCursorDown();
            TerminalUtils::clearPrompt();
        }
        for ($i = 0; $i < $linesUsed; $i++) {
            TerminalUtils::moveCursorUp();
        }

        $this->tabMatchesDisplayed = false;
    }

    private function visibleLength(string $str): int {
        return mb_strlen(preg_replace('/\033\[[0-9;]*m/', '', $str));
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
            $max = min(mb_strlen($prefix), mb_strlen($str));
            while ($i < $max && $prefix[$i] === $str[$i]) $i++;
            $prefix = mb_substr($prefix, 0, $i);
            if ($prefix === "") break;
        }

        return $prefix;
    }

    private function readChar(int $timeoutMs): ?string {
        $this->ensureOpen();

        if ($this->pendingChar !== null) {
            $char = $this->pendingChar;
            $this->pendingChar = null;
            return $char;
        }

        $read = [STDIN];
        $write = null;
        $except = null;
        $tv_usec = ($timeoutMs % 1000) * 1000;

        if (stream_select($read, $write, $except, 0, $tv_usec) <= 0) return null;

        $char = fread(STDIN, 1);
        if ($char === false || $char === "") return null;

        $ord = ord($char);
        if ($ord < 0x80) return $char;
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
                if ($this->historyIndex > 0 && $this->historyEnabled) {
                    $this->historyIndex--;
                    $this->input = $this->history[$this->historyIndex];
                }

                $this->cursor = mb_strlen($this->input);
                $this->redraw($prompt);
                break;
            case "[B": // Down
                if ($this->historyEnabled) {
                    if ($this->historyIndex < (count($this->history) - 1)) {
                        $this->historyIndex++;
                        $this->input = $this->history[$this->historyIndex];
                    } else {
                        $this->input = "";
                        $this->historyIndex = count($this->history);
                    }
                }

                $this->cursor = mb_strlen($this->input);
                $this->redraw($prompt);
                break;
            case "[C": // Right
                if ($this->cursor < mb_strlen($this->input)) $this->cursor++;
                break;
            case "[D": // Left
                if ($this->cursor > 0) $this->cursor--;
                break;
        }
    }

    private function redraw(string $prompt): void {
        $display = $this->visibleTyping ? $this->input : "";
        $back = mb_strlen($display) - $this->cursor;
        $out = "\r\033[2K" . $prompt . $display;
        if ($back > 0) $out .= "\033[{$back}D";
        TerminalUtils::write($out);
    }

    public function println(string $message): void {
        TerminalUtils::clearPrompt();
        $wasDisplayed = $this->tabMatchesDisplayed;
        if ($wasDisplayed) {
            $linesUsed = $this->matchesLineCount();
            for ($i = 0; $i < $linesUsed; $i++) {
                TerminalUtils::moveCursorDown();
                TerminalUtils::clearPrompt();
            }
            for ($i = 0; $i < $linesUsed; $i++) {
                TerminalUtils::moveCursorUp();
            }
            $this->tabMatchesDisplayed = false;
        }

        TerminalUtils::write($message . PHP_EOL);
        $this->redraw($this->prompt);

        if ($wasDisplayed) {
            $this->displayTabMatches();
        }
    }

    public function dump(mixed ...$vars): void {
        ob_start();
        var_dump(...$vars);
        $out = str_replace("\t", "    ", ob_get_clean());

        $wasDisplayed = $this->tabMatchesDisplayed;
        if ($wasDisplayed) {
            TerminalUtils::clearPrompt();
            $linesUsed = $this->matchesLineCount();
            for ($i = 0; $i < $linesUsed; $i++) {
                TerminalUtils::moveCursorDown();
                TerminalUtils::clearPrompt();
            }
            for ($i = 0; $i < $linesUsed; $i++) {
                TerminalUtils::moveCursorUp();
            }
            $this->tabMatchesDisplayed = false;
        }

        foreach (explode(PHP_EOL, $out) as $line) {
            TerminalUtils::clearPrompt();
            TerminalUtils::write($line . PHP_EOL);
        }

        $this->redraw($this->prompt);

        if ($wasDisplayed) {
            $this->displayTabMatches();
        }
    }

    public function ensureOpen(): void {
        if ($this->closed) throw new RuntimeException("Console is already closed");
    }

    public function close(): void {
        $this->ensureOpen();
        shell_exec("stty " . $this->oldSttySettings);
        $this->closed = true;
        $this->history = [];
        $this->historyIndex = 0;
        $this->prompt = "";
        $this->input = "";
        TerminalUtils::clearPrompt();
    }

    public function __destruct() {
        shell_exec("stty " . $this->oldSttySettings);
    }

    public function setPrompt(string $prompt): void {
        $this->prompt = $prompt;
        $this->redraw($prompt);
    }

    public function setCompletionCallback(Closure $callback): void {
        $this->completionCallback = $callback;
    }

    public function setControlCHandler(?Closure $controlCHandler): void {
        $this->controlCHandler = $controlCHandler;
    }

    public function setInput(string $input): void {
        $this->input = $input;
        $this->cursor = mb_strlen($this->input);
        $this->redraw($this->prompt);
    }

    public function isClosed(): bool {
        return $this->closed;
    }

    public function getOldSttySettings(): string {
        return $this->oldSttySettings;
    }

    public function isTypingEnabled(): bool {
        return $this->typingEnabled;
    }

    public function isVisibleTyping(): bool {
        return $this->visibleTyping;
    }

    public function isHistoryEnabled(): bool {
        return $this->historyEnabled;
    }

    public function getHistory(): array {
        return $this->history;
    }

    public function getHistoryIndex(): int {
        return $this->historyIndex;
    }

    public function getCursor(): int {
        return $this->cursor;
    }

    public function getInput(): string {
        return $this->input;
    }

    public function isPressedEnter(): bool {
        return $this->pressedEnter;
    }

    public function getTabMatches(): array {
        return $this->tabMatches;
    }

    public function getTabIndex(): int {
        return $this->tabIndex;
    }

    public function isTabActive(): bool {
        return $this->tabActive;
    }

    public function isTabMatchesDisplayed(): bool {
        return $this->tabMatchesDisplayed;
    }

    public function getPrompt(): string {
        return $this->prompt;
    }

    public function getCompletionCallback(): ?Closure {
        return $this->completionCallback;
    }

    public function getControlCHandler(): ?Closure {
        return $this->controlCHandler;
    }
}