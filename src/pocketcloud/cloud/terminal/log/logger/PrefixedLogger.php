<?php

namespace pocketcloud\cloud\terminal\log\logger;

use pocketcloud\cloud\terminal\log\level\CloudLogLevel;

final class PrefixedLogger extends Logger {

    public function __construct(
        private readonly Logger $logger,
        private string $prefix
    ) {
        parent::__construct();
        $this->close();
    }

    public function send(CloudLogLevel $logLevel, string $message, string ...$params): Logger {
        return $this->logger->send($logLevel, $this->prefix . " $message", ...$params);
    }

    public function setPrefix(string $prefix): void {
        $this->prefix = $prefix;
    }

    public function getPrefix(): string {
        return $this->prefix;
    }
}