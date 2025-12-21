<?php

namespace pocketcloud\cloud\console\command\sender;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\exception\NoLoggerAvailableException;

final class ConsoleCommandSender implements ICommandSender {

    /**
     * @throws NoLoggerAvailableException
     */
    public function info(string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->info($message, ...$params);
        return $this;
    }

    /**
     * @throws NoLoggerAvailableException
     */
    public function warn(string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->warn($message, ...$params);
        return $this;
    }

    /**
     * @throws NoLoggerAvailableException
     */
    public function error(string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->error($message, ...$params);
        return $this;
    }

    /**
     * @throws NoLoggerAvailableException
     */
    public function success(string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->success($message, ...$params);
        return $this;
    }

    /**
     * @throws NoLoggerAvailableException
     */
    public function debug(string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->debug($message, ...$params);
        return $this;
    }

    /**
     * @throws NoLoggerAvailableException
     */
    public function log(CloudLogLevel $logLevel, string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->log($logLevel, $message, ...$params);
        return $this;
    }
}