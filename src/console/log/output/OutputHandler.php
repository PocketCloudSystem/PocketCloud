<?php

namespace pocketcloud\cloud\console\log\output;

use pocketcloud\cloud\console\log\logger\ILogger;

interface OutputHandler {

    public function shouldOutput(ILogger $logger): bool;

    public function handleOutput(string $message): void;
}