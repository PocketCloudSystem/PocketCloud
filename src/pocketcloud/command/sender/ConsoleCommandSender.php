<?php

namespace pocketcloud\command\sender;

use pocketcloud\util\CloudLogger;

class ConsoleCommandSender implements ICommandSender {

    public function info(string $message, string ...$params): ICommandSender {
        CloudLogger::get()->info($message, ...$params);
        return $this;
    }

    public function warn(string $message, string ...$params): ICommandSender {
        CloudLogger::get()->warn($message, ...$params);
        return $this;
    }

    public function error(string $message, string ...$params): ICommandSender {
        CloudLogger::get()->error($message, ...$params);
        return $this;
    }

    public function debug(string $message, string ...$params): ICommandSender {
        CloudLogger::get()->debug($message, ...$params);
        return $this;
    }
}