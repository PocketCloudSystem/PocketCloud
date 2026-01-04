<?php

namespace pocketcloud\cloud\console\command\impl\plugin;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;

final class PluginCommand extends Command {

    public function __construct() {
        parent::__construct("plugin", "Manage the plugins", ["pl"]);
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }
}