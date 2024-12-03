<?php

namespace pocketcloud\cloud\command\sender;

interface ICommandSender {

    public function info(string $message, string ...$params): self;

    public function warn(string $message, string ...$params): self;

    public function error(string $message, string ...$params): self;

    public function debug(string $message, string ...$params): self;
}