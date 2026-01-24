<?php

namespace pocketcloud\cloud\console\log\output;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\logger\ILogger;

final class MonitorOutputHandler implements OutputHandler {

    public function shouldOutput(ILogger $logger): bool {
        return false;
    }

    public function handleOutput(string $message): void {
        Console::getInstance()->println($message);
    }
}