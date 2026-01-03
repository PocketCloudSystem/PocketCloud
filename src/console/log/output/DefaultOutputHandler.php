<?php

namespace pocketcloud\cloud\console\log\output;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\logger\ILogger;

final class DefaultOutputHandler implements OutputHandler {

    public function shouldOutput(ILogger $logger): bool {
        return true;
    }

    public function handleOutput(string $message): void {
        Console::getInstance()->println($message);
    }
}