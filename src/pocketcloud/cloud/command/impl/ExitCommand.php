<?php

namespace pocketcloud\cloud\command\impl;

use pocketcloud\cloud\command\argument\def\BoolArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\PocketCloud;

final class ExitCommand extends Command {

    public function __construct() {
        parent::__construct("exit", "Stop the cloud");
        $this->addParameter(new BoolArgument(
            "confirmation",
            false
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        if ($args["confirmation"]) PocketCloud::getInstance()->shutdown();
        return true;
    }
}