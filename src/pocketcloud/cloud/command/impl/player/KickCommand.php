<?php

namespace pocketcloud\cloud\command\impl\player;

use pocketcloud\cloud\command\argument\def\PlayerArgument;
use pocketcloud\cloud\command\argument\def\StringArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;

final class KickCommand extends Command {

    public function __construct() {
        parent::__construct("kick", "Kick a player");

        $this->addParameter(new PlayerArgument(
            "player",
            false,
            "The player was not found."
        ));

        $this->addParameter(new StringArgument(
            "reason",
            true,
            true
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $player = $args["player"];
        $reason = $args["reason"] ?? "";

        $sender->success("The player has been successfully §ckicked§r!");
        $player->kick($reason);
        return true;
    }
}