<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;

final class ConfigureCommand extends Command {

    public function __construct() {
        parent::__construct("configure", "Reconfigure the config", ["conf", "reconf", "reconfigure"]);
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }
}