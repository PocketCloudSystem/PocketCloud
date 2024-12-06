<?php

namespace pocketcloud\cloud\command\sender;

use pocketcloud\cloud\terminal\log\CloudLogger;

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

    public function success(string $message, string ...$params): ICommandSender {
        CloudLogger::get()->success($message, ...$params);
        return $this;
    }

    public function debug(string $message, string ...$params): ICommandSender {
        CloudLogger::get()->debug($message, ...$params);
        return $this;
    }
}