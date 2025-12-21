<?php

namespace pocketcloud\cloud\console\log\logger;

use DateInvalidTimeZoneException;
use DateMalformedStringException;
use DateTime;
use DateTimeZone;
use pmmp\thread\Thread;
use pmmp\thread\ThreadSafe;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\console\log\color\CloudConsoleColor;
use pocketcloud\cloud\exception\NoLoggerAvailableException;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\util\FileUtils;
use pocketmine\snooze\SleeperHandlerEntry;
use ReflectionClass;
use ReflectionException;
use Throwable;

class ThreadLogger extends ThreadSafe implements ILogger {

    public function __construct(
        private readonly ThreadSafeArray $buffer,
        private readonly SleeperHandlerEntry $entry
    ) {}

    protected function addLogToBuffer(string $formattedMessage): void {
        $this->buffer->synchronized(function (string $formattedMessage): void {
            $this->buffer[] = $formattedMessage;
            $this->entry->createNotifier()->wakeupSleeper();
        }, $formattedMessage);
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
        return $this;
    }

    public function forceDebug(string $message, string ...$params): self {
        return $this->log(CloudLogLevel::DEBUG(), $message, ...$params);
    }

    public function exception(Throwable $throwable): self {
        $this->error("§cUnhandled §e{}§c: §e{} §cwas thrown in §e{} §cat line §e{}", $throwable::class, $throwable->getMessage(), FileUtils::cleanPath($throwable->getFile()), $throwable->getLine());
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
                $this->error("§cTrace §e#{} §ccalled at '§e{}({})§c' in §e{} §cat line §e{}", $i, $trace["function"], $args, FileUtils::cleanPath($trace["file"] ?? $trace["class"]), $trace["line"]);
            } else {
                $this->error("§cTrace §e#{} §ccalled at '§e{}({})§c' in §e{}", $i, $trace["function"], $args, FileUtils::cleanPath($trace["file"] ?? $trace["class"]));
            }
            $i++;
        }

        return $this;
    }


    /**
     * @throws NoLoggerAvailableException
     */
    public function log(CloudLogLevel $logLevel, string $message, ...$params): self {
        if (Thread::getCurrentThread() === null) throw new NoLoggerAvailableException("A instance of ThreadLogger can't be used to log outside of a thread");
        try {
            $time = new DateTime("now", new DateTimeZone(ini_get("date.timezone")));
        } catch (DateInvalidTimeZoneException|DateMalformedStringException) {
            $time = new DateTime();
        }

        $threadName = "Unknown thread";
        try {
            if (Thread::getCurrentThread() !== null) {
                if (method_exists(Thread::getCurrentThread(), "getThreadName")) $threadName = Thread::getCurrentThread()->getThreadName();
                else $threadName = new ReflectionClass(Thread::getCurrentThread())->getShortName();
            }
        } catch (ReflectionException) {}

        $parsedMessage = count($params) > 0 ? sprintf($message, ...$params) : $message;
        $format = str_replace(
            ["{thread}", "{time}", "{time_with_ms}", "{log_level}", "{message}"],
            [$threadName, $time->format("H:i:s"), $time->format("H:i:s.v"), $logLevel->getPrefix(), $parsedMessage],
            $this->customFormat ?? Logger::LOG_FORMAT
        );

        $this->addLogToBuffer(CloudConsoleColor::toColoredString($format));
        return $this;
    }

    public function emptyLine(bool $prefix = false, ?CloudLogLevel $logLevel = null): self {
        if ($prefix) {
            $this->log($logLevel ?? CloudLogLevel::INFO(), "");
        } else {
            $this->addLogToBuffer("\r");
        }

        return $this;
    }

    public function echo(string $message): void {
        $this->addLogToBuffer($message);
    }

    public function dump(mixed ...$vars): void {

    }
}