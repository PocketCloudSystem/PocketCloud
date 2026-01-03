<?php

namespace pocketcloud\cloud\console\log\output;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\logger\ILogger;

final class SetupOutputHandler implements OutputHandler {

    private array $authorizedLoggers = [];

    public function addAuthorizedLogger(ILogger $logger): void {
        $this->authorizedLoggers[spl_object_id($logger)] = true;
    }

    public function removeAuthorizedLogger(ILogger $logger): void {
        unset($this->authorizedLoggers[spl_object_id($logger)]);
    }

    public function shouldOutput(ILogger $logger): bool {
        return isset($this->authorizedLoggers[spl_object_id($logger)]);
    }

    public function handleOutput(string $message): void {
        Console::getInstance()->println($message);
    }
}