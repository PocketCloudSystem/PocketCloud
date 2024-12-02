<?php

namespace pocketcloud\cloud\terminal\log\logger;

use pocketcloud\cloud\terminal\log\color\CloudColor;
use pocketcloud\cloud\terminal\log\level\CloudLogLevel;
use pocketcloud\cloud\util\Utils;
use ReflectionClass;
use Throwable;

final class Logger {

    private mixed $cloudLogFile;
    private bool $closed = false;

    public function __construct(
        private readonly ?string $cloudLogPath = null,
        private bool $debugMode = false
    ) {
        $this->cloudLogFile = fopen($this->cloudLogPath ?? LOG_PATH, "ab");
    }

    public function info(string $message, string ...$params): self {
        return $this->send(CloudLogLevel::INFO(), $message, ...$params);
    }

    public function warn(string $message, string ...$params): self {
        return $this->send(CloudLogLevel::WARN(), $message, ...$params);
    }

    public function error(string $message, string ...$params): self {
        return $this->send(CloudLogLevel::ERROR(), $message, ...$params);
    }

    public function debug(string $message, bool $force = false, string ...$params): self {
        if ($this->isDebugMode() || $force) return $this->send(CloudLogLevel::DEBUG(), $message, ...$params);
        return $this;
    }

    public function exception(Throwable $throwable): self {
        $this->error("§cUnhandled §e%s§c: §e%s §cwas thrown in §e%s §cat line §e%s", $throwable::class, $throwable->getMessage(), Utils::cleanPath($throwable->getFile()), $throwable->getLine());
        $i = 1;
        foreach ($throwable->getTrace() as $trace) {
            $args = implode(", ", array_map(function(mixed $argument): string {
                if (is_object($argument)) {
                    return (new ReflectionClass($argument))->getShortName();
                } else if (is_array($argument)) {
                    return "array(" . count($argument) . ")";
                }
                return gettype($argument);
            }, ($trace["args"] ?? [])));

            if (isset($trace["line"])) {
                $this->error("§cTrace §e#%s §ccalled at '§e%s(%s)§c' in §e%s §cat line §e%s", $i, $trace["function"], $args, Utils::cleanPath($trace["file"] ?? $trace["class"]), $trace["line"]);
            } else {
                $this->error("§cTrace §e#%s §ccalled at '§e%s(%s)§c' in §e%s", $i, $trace["function"], $args, Utils::cleanPath($trace["file"] ?? $trace["class"]));
            }
            $i++;
        }
        return $this;
    }

    public function send(CloudLogLevel $logLevel, string $message, string ...$params): self {
        $format = CloudColor::YELLOW() . date("Y-m-d H:i:s") . CloudColor::DARK_GRAY() . " | " . CloudColor::RESET() . $logLevel->getPrefix() . CloudColor::DARK_GRAY() . " » " . CloudColor::RESET() . (empty($params) ? $message : sprintf($message, ...$params)) . CloudColor::RESET();
        $line = CloudColor::toColoredString($format) . "\n";

        echo $line;
        LoggingCache::save($line);
        $this->write($format . "\n");

        return $this;
    }

    public function emptyLine(bool $prefix = false): self {
        if ($prefix) {
            $this->send(CloudLogLevel::INFO(), "");
        } else echo "\r\n";
        return $this;
    }

    private function write(string $message): void {
        if (!$this->closed) fwrite($this->cloudLogFile, mb_convert_encoding($message, "UTF-8"));
    }

    public function close(): void {
        if (!$this->closed) {
            $this->closed = true;
            if ($this->cloudLogFile !== null) fclose($this->cloudLogFile);
        }
    }

    public function setDebugMode(bool $debugMode): void {
        $this->debugMode = $debugMode;
    }

    public function isDebugMode(): bool {
        return $this->debugMode;
    }
}