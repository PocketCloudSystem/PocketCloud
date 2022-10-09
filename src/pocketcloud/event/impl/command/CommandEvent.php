<?php

namespace pocketcloud\event\impl\command;

use pocketcloud\command\Command;
use pocketcloud\event\Event;

abstract class CommandEvent extends Event {

    public function __construct(private Command $command) {}

    public function getCommand(): Command {
        return $this->command;
    }
}