<?php

namespace pocketcloud\console\log;

use pocketcloud\console\log\color\CloudColor;
use pocketcloud\console\log\level\CloudLogLevel;
use pocketcloud\utils\Utils;

class Logger {

    private mixed $cloudLogFile;
    private bool $closed = false;

    public function __construct(private string $cloudLogPath, private bool $debugMode = true) {
        $this->cloudLogFile = fopen($this->cloudLogPath, "ab");
    }

    public function info(string $message): void {
        $this->send(CloudLogLevel::INFO(), $message);
    }

    public function warn(string $message): void {
        $this->send(CloudLogLevel::WARN(), $message);
    }

    public function error(string $message): void {
        $this->send(CloudLogLevel::ERROR(), $message);
    }

    public function debug(string $message, bool $force = false): void {
        if ($this->isDebugMode() && !$force) $this->send(CloudLogLevel::DEBUG(), $message);
    }

    public function exception(\Throwable $throwable) {
        $this->error("§cUnhandled §e" . $throwable::class . "§c: §e" . $throwable->getMessage() . " §cwas thrown in §e" . Utils::cleanPath($throwable->getFile()) . " §cat line §e" . $throwable->getLine());
        $i = 1;
        foreach ($throwable->getTrace() as $trace) {
            $args = implode(", ", array_map(function(mixed $argument): string {
                if (is_object($argument)) {
                    return (new \ReflectionClass($argument))->getShortName();
                } else if (is_array($argument)) {
                    return "array(" . count($argument) . ")";
                }
                return gettype($argument);
            }, ($trace["args"] ?? [])));

            $this->error("§cTrace §e#$i §ccalled at '§e" . $trace["function"] . "(" . $args . ")§c' in §e" . Utils::cleanPath($trace["file"] ?? $trace["class"]) . (isset($trace["line"]) ? " §cat line §e" . $trace["line"] : ""));
            $i++;
        }
    }

    public function send(CloudLogLevel $logLevel, string $message): void {
        $format = CloudColor::YELLOW() . date("Y-m-d H:i:s") . CloudColor::DARK_GRAY() . " | " . CloudColor::RESET() . $logLevel->getPrefix() . CloudColor::DARK_GRAY() . " » " . CloudColor::RESET() . $message . CloudColor::RESET();
        $this->write(CloudColor::toColoredString($format, false) . "\n");
        echo CloudColor::toColoredString($format, true) . "\n";
    }

    public function emptyLine(bool $prefix = false): void {
        if ($prefix) $this->send(CloudLogLevel::INFO(), "");
        else echo "\n\r";
    }

    public function write(string $message): void {
        if (!$this->closed) fwrite($this->cloudLogFile, $message);
    }

    public function close(): void {
        if (!$this->closed) {
            $this->closed = true;
            fclose($this->cloudLogFile);
        }
    }

    public function isDebugMode(): bool {
        return $this->debugMode;
    }
}