<?php

namespace pocketcloud\cloud\console\log\logger;

use pocketcloud\cloud\console\log\level\CloudLogLevel;
use Throwable;

interface ILogger {

    public function info(string $message, string ...$params): self;

    public function warn(string $message, string ...$params): self;

    public function error(string $message, string ...$params): self;

    public function success(string $message, string ...$params): self;

    public function debug(string $message, string ...$params): self;

    public function forceDebug(string $message, string ...$params): self;

    public function exception(Throwable $throwable): self;

    public function log(CloudLogLevel $logLevel, string $message, mixed... $params): self;

    public function emptyLine(bool $prefix = false, ?CloudLogLevel $logLevel = null): self;

    public function echo(string $message): void;

    public function dump(mixed ...$vars): void;
}