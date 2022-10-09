<?php

namespace pocketcloud\command\impl\server;

use pocketcloud\command\Command;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;
use pocketcloud\utils\CloudLogger;

class StopCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0])) {
            if (($template = TemplateManager::getInstance()->getTemplateByName($args[0])) !== null) {
                CloudServerManager::getInstance()->stopTemplate($template);
            } else if (($server = CloudServerManager::getInstance()->getServerByName($args[0])) !== null) {
                CloudServerManager::getInstance()->stopServer($server);
            } else if (strtolower($args[0]) == "all") {
                if (empty(CloudServerManager::getInstance()->getServers())) {
                    CloudLogger::get()->error("§cNo servers available!");
                    return true;
                }
                CloudServerManager::getInstance()->stopAll();
            } else CloudLogger::get()->error("§cThe server doesn't exists!");
        } else return false;
        return true;
    }
}