<?php

namespace pocketcloud\cloud\console\log\output\impl;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\logger\ILogger;
use pocketcloud\cloud\console\log\output\OutputHandler;

final class DefaultOutputHandler implements OutputHandler {

    public function shouldOutput(ILogger $logger): bool {
        return true;
    }

    public function handleOutput(string $message): void {
        Console::getInstance()->println($message);
    }
}