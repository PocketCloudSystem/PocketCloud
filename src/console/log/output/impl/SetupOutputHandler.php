<?php

namespace pocketcloud\cloud\console\log\output\impl;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\logger\ILogger;
use pocketcloud\cloud\console\log\output\OutputHandler;
use pocketcloud\cloud\console\log\output\trait\AuthorizedLoggerBase;

final class SetupOutputHandler implements OutputHandler {
    use AuthorizedLoggerBase;

    public function shouldOutput(ILogger $logger): bool {
        return $this->isAuthorizedLogger($logger);
    }

    public function handleOutput(string $message): void {
        Console::getInstance()->println($message);
    }
}