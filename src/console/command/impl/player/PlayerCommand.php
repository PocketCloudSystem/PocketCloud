<?php

namespace pocketcloud\cloud\console\command\impl\player;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;

final class PlayerCommand extends Command {

    public function __construct() {
        parent::__construct("player", "Manage the players", ["ppl"]);
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        return true;
    }
}