<?php

namespace pocketcloud\cloud\terminal\log\logger;

use pocketcloud\cloud\terminal\log\color\CloudColor;
use pocketcloud\cloud\terminal\log\level\CloudLogLevel;
use pocketcloud\cloud\thread\Thread;
use pocketcloud\cloud\thread\Worker;
use pocketcloud\cloud\util\Utils;
use ReflectionClass;
use ReflectionException;
use Throwable;

final class Logger {

    private mixed $cloudLogFile;
    private bool $closed = false;
    private bool $saveLogs = true;

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
                    try {
                        return (new ReflectionClass($argument))->getShortName();
                    } catch (ReflectionException) {
                        return get_class($argument);
                    }
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
        $threadName = "";
        try {
            if (Thread::getCurrentThread() !== null) {
                $threadName = "§8[§c" . (new ReflectionClass(Thread::getCurrentThread()))->getShortName() . "§8] ";
            }
        } catch (ReflectionException) {}

        $format = $threadName . "§8[§e" . date("Y-m-d H:i:s") . "§7/§r" . $logLevel->getPrefix() . "§r§8] §r" . (empty($params) ? $message : sprintf($message, ...$params)) . CloudColor::RESET();
        $line = CloudColor::toColoredString($format) . "\n";

        echo $line;
        if ($this->saveLogs) {
            LoggingCache::save($line);
            $this->write($format . "\n");
        }

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

    public function setSaveLogs(bool $saveLogs): void {
        $this->saveLogs = $saveLogs;
    }

    public function isSaveLogs(): bool {
        return $this->saveLogs;
    }
}