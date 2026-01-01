<?php

namespace pocketcloud\cloud\console\command\sender;

use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\log\level\CloudLogLevel;

final class ConsoleCommandSender implements ICommandSender {

    public function info(string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->info($message, ...$params);
        return $this;
    }

    public function warn(string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->warn($message, ...$params);
        return $this;
    }

    public function error(string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->error($message, ...$params);
        return $this;
    }

    public function success(string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->success($message, ...$params);
        return $this;
    }

    public function debug(string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->debug($message, ...$params);
        return $this;
    }

    public function log(CloudLogLevel $logLevel, string $message, mixed ...$params): ICommandSender {
        CloudLogger::get()->log($logLevel, $message, ...$params);
        return $this;
    }
}