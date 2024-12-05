<?php

namespace pocketcloud\cloud\command\impl\server;

use pocketcloud\cloud\command\argument\def\ServerArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\server\CloudServerManager;

class SaveCommand extends Command {

    public function __construct() {
        parent::__construct("save", "Save a server");

        $this->addParameter(new ServerArgument(
            "server",
            false
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        CloudServerManager::getInstance()->save($args["server"]);
        return true;
    }
}