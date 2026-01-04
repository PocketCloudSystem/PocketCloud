<?php

namespace pocketcloud\cloud\console\command\impl\group;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;

final class GroupCommand extends Command {

    public function __construct() {
        parent::__construct("group", "Manage server groups", ["g", "sg", "servergroup"]);
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }
}