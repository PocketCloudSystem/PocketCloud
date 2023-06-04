<?php

namespace pocketcloud\command\impl\player;

use pocketcloud\command\Command;
use pocketcloud\language\Language;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\util\CloudLogger;

class KickCommand extends Command {

    public function execute(string $label, array $args): bool {
        if (isset($args[0])) {
            if (($player = CloudPlayerManager::getInstance()->getPlayerByName(array_shift($args))) !== null) {
                CloudLogger::get()->info(Language::current()->translate("command.kick.success", $player->getName()));
                $player->kick(implode(" ", $args));
            } else CloudLogger::get()->error(Language::current()->translate("command.kick.failed"));
        } else return false;
        return true;
    }
}