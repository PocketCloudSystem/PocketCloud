<?php

namespace pocketcloud\cloud\event\impl\command;

use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\event\Event;

abstract class CommandEvent extends Event {

    public function __construct(private readonly Command $command) {}

    public function getCommand(): Command {
        return $this->command;
    }
}