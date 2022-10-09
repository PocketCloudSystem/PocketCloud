<?php

namespace pocketcloud\command\impl\server;

use pocketcloud\command\Command;
use pocketcloud\server\CloudServerManager;
use pocketcloud\utils\CloudLogger;

class ExecuteCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0]) && isset($args[1])) {
            if (($server = CloudServerManager::getInstance()->getServerByName(array_shift($args))) !== null) {
                CloudLogger::get()->info("The command was sent to §e" . $server->getName() . "§r!");
                CloudServerManager::getInstance()->sendCommand($server, implode(" ", $args));
            } else CloudLogger::get()->error("§cThe server doesn't exists!");
        } else return false;
        return true;
    }
}