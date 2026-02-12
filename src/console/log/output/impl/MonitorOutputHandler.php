<?php

namespace pocketcloud\cloud\console\log\output\impl;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\logger\ILogger;
use pocketcloud\cloud\console\log\output\OutputHandler;

final class MonitorOutputHandler implements OutputHandler {

    public function shouldOutput(ILogger $logger): bool {
        return false;
    }

    public function handleOutput(string $message): void {
        Console::getInstance()->println($message);
    }
}