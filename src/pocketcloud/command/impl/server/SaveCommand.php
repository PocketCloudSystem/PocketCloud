<?php

namespace pocketcloud\command\impl\server;

use pocketcloud\command\Command;
use pocketcloud\server\CloudServerManager;
use pocketcloud\utils\CloudLogger;

class SaveCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0])) {
            if (($server = CloudServerManager::getInstance()->getServerByName($args[0])) !== null) {
                CloudServerManager::getInstance()->saveServer($server);
            } else CloudLogger::get()->error("§cThe server doesn't exists!");
        } else return false;
        return true;
    }
}