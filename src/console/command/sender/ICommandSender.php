<?php

namespace pocketcloud\cloud\console\command\sender;

use pocketcloud\cloud\console\log\level\CloudLogLevel;

interface ICommandSender {

    public function info(string $message, mixed ...$params): self;

    public function warn(string $message, mixed ...$params): self;

    public function error(string $message, mixed ...$params): self;

    public function success(string $message, mixed ...$params): self;

    public function debug(string $message, mixed ...$params): self;

    public function log(CloudLogLevel $logLevel, string $message, mixed ...$params): self;
}