<?php

namespace pocketcloud\command\impl\player;

use pocketcloud\command\Command;
use pocketcloud\player\CloudPlayerManager;
use pocketcloud\utils\CloudLogger;

class KickCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0])) {
            if (($player = CloudPlayerManager::getInstance()->getPlayerByName(array_shift($args))) !== null) {
                CloudLogger::get()->info("The player §e" . $player->getName() . " §rwas kicked!");
                $player->kick(implode(" ", $args));
            } else CloudLogger::get()->error("§cThe player isn't online!");
        } else return false;
        return true;
    }
}