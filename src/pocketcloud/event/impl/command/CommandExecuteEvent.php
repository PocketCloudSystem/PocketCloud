<?php

namespace pocketcloud\event\impl\command;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;

class CommandExecuteEvent extends CommandEvent {

    public function __construct(
        private readonly ICommandSender $sender,
        Command $command
    ) {
        parent::__construct($command);
    }

    public function getSender(): ICommandSender {
        return $this->sender;
    }
}