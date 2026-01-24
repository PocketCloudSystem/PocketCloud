<?php

namespace pocketcloud\cloud\console\log\logger;

use DateInvalidTimeZoneException;
use DateMalformedStringException;
use DateTime;
use DateTimeZone;
use pmmp\thread\Thread;
use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\color\CloudConsoleColor;
use pocketcloud\cloud\console\log\logger\cache\LogMessagesCache;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\console\log\output\OutputManager;
use pocketcloud\cloud\util\FormatUtils;
use pocketcloud\cloud\util\PathUtils;
use ReflectionClass;
use ReflectionException;
use Throwable;

class MainLogger implements ILogger {

    public const string LOG_FORMAT = "§8[§b{time_with_ms}§8] §8[§r{thread}§8/§r{log_level}§r§8] §r{message}§r";

    protected bool $closed = false;
    protected ?string $format = null;
    protected mixed $logFile = null;

    public function __construct(
        protected readonly ?string $cloudLogPath,
        protected bool $debugMode,
        protected bool $saveLogs
    ) {
        if ($this->cloudLogPath !== null) {
            $this->logFile = fopen($this->cloudLogPath, "ab");
        }
    }

    public function info(string $message, string ...$params): self {
        return $this->log(CloudLogLevel::INFO(), $message, ...$params);
    }

    public function warn(string $message, string ...$params): self {
        return $this->log(CloudLogLevel::WARN(), $message, ...$params);
    }

    public function error(string $message, string ...$params): self {
        return $this->log(CloudLogLevel::ERROR(), $message, ...$params);
    }

    public function success(string $message, string ...$params): self {
        return $this->log(CloudLogLevel::SUCCESS(), $message, ...$params);
    }

    public function debug(string $message, string ...$params): self {
        if ($this->debugMode) return $this->log(CloudLogLevel::DEBUG(), $message, ...$params);
        return $this;
    }

    public function forceDebug(string $message, string ...$params): self {
        return $this->log(CloudLogLevel::DEBUG(), $message, ...$params);
    }

    public function exception(Throwable $throwable): self {
        $this->error("§cUnhandled §e{}§c: §e{} §cwas thrown in §e{} §cat line §e{}", $throwable::class, $throwable->getMessage(), PathUtils::clean($throwable->getFile()), $throwable->getLine());
        $i = 1;
        foreach ($throwable->getTrace() as $trace) {
            $args = implode(", ", array_map(function(mixed $argument): string {
                if (is_object($argument)) {
                    try {
                        return new ReflectionClass($argument)->getShortName();
                    } catch (ReflectionException) {
                        return get_class($argument);
                    }
                } else if (is_array($argument)) {
                    return "array(" . count($argument) . ")";
                }
                return gettype($argument);
            }, ($trace["args"] ?? [])));

            if (isset($trace["line"])) {
                $this->error("§cTrace §e#{} §ccalled at '§e{}({})§c' in §e{} §cat line §e{}", $i, $trace["function"], $args, PathUtils::clean($trace["file"] ?? $trace["class"]), $trace["line"]);
            } else {
                $this->error("§cTrace §e#{} §ccalled at '§e{}({})§c' in §e{}", $i, $trace["function"], $args, PathUtils::clean($trace["file"] ?? $trace["class"]));
            }
            $i++;
        }

        return $this;
    }

    public function log(CloudLogLevel $logLevel, string $message, mixed... $params): self {
        try {
            $time = new DateTime("now", new DateTimeZone(ini_get("date.timezone")));
        } catch (DateInvalidTimeZoneException|DateMalformedStringException) {
            $time = new DateTime();
        }

        $threadName = "Main thread";
        try {
            if (Thread::getCurrentThread() !== null) {
                if (method_exists(Thread::getCurrentThread(), "getThreadName")) $threadName = Thread::getCurrentThread()->getThreadName();
                else $threadName = new ReflectionClass(Thread::getCurrentThread())->getShortName();
            }
        } catch (ReflectionException) {}

        $parsedMessage = count($params) > 0 ? FormatUtils::interpolate($message, $params) : $message;
        $format = str_replace(
            ["{thread}", "{time}", "{time_with_ms}", "{log_level}", "{message}"],
            [$threadName, $time->format("H:i:s"), $time->format("H:i:s.v"), $logLevel->getPrefix(), $parsedMessage],
            $this->format ?? self::LOG_FORMAT
        );
        $line = CloudConsoleColor::toColoredString($format);

        if (OutputManager::getHandler()->shouldOutput($this)) {
            $this->echo($line);
        }

        if ($this->saveLogs) {
            LogMessagesCache::save($line);
            $this->write($format . PHP_EOL);
        }

        return $this;
    }

    /** @internal */
    public function echo(string $message): void {
        OutputManager::getHandler()->handleOutput($message);
    }

    public function dump(mixed ...$vars): void {
        Console::getInstance()->dump(...$vars);
    }

    public function emptyLine(bool $prefix = false, ?CloudLogLevel $logLevel = null): self {
        if ($prefix) {
            $this->log($logLevel ?? CloudLogLevel::INFO(), "");
        } else {
            if (OutputManager::getHandler()->shouldOutput($this)) {
                $this->echo("");
            }

            if ($this->saveLogs) {
                LogMessagesCache::save("");
                $this->write("\r" . PHP_EOL);
            }
        }

        return $this;
    }

    protected function write(string $message): void {
        if (!$this->closed && $this->logFile !== null) fwrite($this->logFile, mb_convert_encoding($message, "UTF-8"));
    }

    public function close(): void {
        if ($this->closed || $this->logFile === null) return;
        $this->closed = true;
        fclose($this->logFile);
        $this->logFile = null;
    }

    public function isClosed(): bool {
        return $this->closed;
    }

    public function resetFormat(): self {
        return $this->setFormat(null);
    }

    public function setFormat(?string $format): self {
        $this->format = $format;
        return $this;
    }

    public function getFormat(): ?string {
        return $this->format;
    }

    public function getLogFile(): mixed {
        return $this->logFile;
    }

    public function setDebugMode(bool $enabled): void {
        $this->debugMode = $enabled;
    }

    public function isDebugMode(): bool {
        return $this->debugMode;
    }

    public function setSaveLogs(bool $enabled): void {
        $this->saveLogs = $enabled;
    }

    public function isSaveLogs(): bool {
        return $this->saveLogs;
    }
}