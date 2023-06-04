<?php

namespace pocketcloud\command\impl\server;

use pocketcloud\command\Command;
use pocketcloud\language\Language;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;
use pocketcloud\util\CloudLogger;

class StopCommand extends Command {

    public function execute(string $label, array $args): bool {
        if (isset($args[0])) {
            if (($template = TemplateManager::getInstance()->getTemplateByName($args[0])) !== null) {
                CloudServerManager::getInstance()->stopTemplate($template);
            } else if (($server = CloudServerManager::getInstance()->getServerByName($args[0])) !== null) {
                CloudServerManager::getInstance()->stopServer($server);
            } else if (strtolower($args[0]) == "all") {
                if (empty(CloudServerManager::getInstance()->getServers())) {
                    CloudLogger::get()->error(Language::current()->translate("command.stop.failed"));
                    return true;
                }
                CloudServerManager::getInstance()->stopAll();
            } else CloudLogger::get()->error(Language::current()->translate("server.not.found"));
        } else return false;
        return true;
    }
}