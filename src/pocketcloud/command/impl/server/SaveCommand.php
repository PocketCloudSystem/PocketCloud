<?php

namespace pocketcloud\command\impl\server;

use pocketcloud\command\Command;
use pocketcloud\language\Language;
use pocketcloud\server\CloudServerManager;
use pocketcloud\util\CloudLogger;

class SaveCommand extends Command {

    public function execute(string $label, array $args): bool {
        if (isset($args[0])) {
            if (($server = CloudServerManager::getInstance()->getServerByName($args[0])) !== null) {
                CloudServerManager::getInstance()->saveServer($server);
            } else CloudLogger::get()->error(Language::current()->translate("server.not.found"));
        } else return false;
        return true;
    }
}