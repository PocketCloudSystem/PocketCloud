<?php

namespace pocketcloud\command\impl\server;

use pocketcloud\command\Command;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;
use pocketcloud\utils\CloudLogger;

class StartCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0])) {
            if (($template = TemplateManager::getInstance()->getTemplateByName($args[0])) !== null) {
                $count = 1;
                if (isset($args[1])) if (is_numeric($args[1])) if (intval($args[1]) > 0) $count = intval($args[1]);

                CloudServerManager::getInstance()->startServer($template, $count);
            } else CloudLogger::get()->error("§cThe template doesn't exists!");
        } else return false;
        return true;
    }
}