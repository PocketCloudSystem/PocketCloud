<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\argument\def\BoolArgument;
use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\PocketCloud;

final class ExitCommand extends Command {

    public function __construct() {
        parent::__construct("exit", "Stop the cloud");
        $this->addParameter(new BoolArgument(
            "confirmation",
            false
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        if ($args["confirmation"]) PocketCloud::getInstance()->shutdown();
        return true;
    }
}