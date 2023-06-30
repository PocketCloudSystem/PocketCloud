<?php

namespace pocketcloud\command\impl\player;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\language\Language;
use pocketcloud\player\CloudPlayerManager;

class KickCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        if (isset($args[0])) {
            if (($player = CloudPlayerManager::getInstance()->getPlayerByName(array_shift($args))) !== null) {
                $sender->info(Language::current()->translate("command.kick.success", $player->getName()));
                $player->kick(implode(" ", $args));
            } else $sender->error(Language::current()->translate("command.kick.failed"));
        } else return false;
        return true;
    }
}