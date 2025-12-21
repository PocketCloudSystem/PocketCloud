<?php

namespace pocketcloud\cloud\console\log\logger;

use pocketcloud\cloud\console\log\level\CloudLogLevel;

final class PrefixedLogger extends Logger {

    public function __construct(
        private readonly Logger $logger,
        private string $prefix
    ) {
        parent::__construct(null, false, false);
        $this->close();
    }

    public function log(CloudLogLevel $logLevel, string $message, ...$params): Logger {
        return $this->logger->log($logLevel, $this->prefix . " $message", ...$params);
    }

    public function setPrefix(string $prefix): void {
        $this->prefix = $prefix;
    }

    public function getPrefix(): string {
        return $this->prefix;
    }
}