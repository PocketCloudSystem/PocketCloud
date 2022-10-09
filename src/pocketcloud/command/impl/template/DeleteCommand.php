<?php

namespace pocketcloud\command\impl\template;

use pocketcloud\command\Command;
use pocketcloud\template\TemplateManager;
use pocketcloud\utils\CloudLogger;

class DeleteCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0])) {
            if (($template = TemplateManager::getInstance()->getTemplateByName($args[0])) !== null) {
                TemplateManager::getInstance()->deleteTemplate($template);
            } else CloudLogger::get()->error("§cThe template doesn't exists!");
        } else return false;
        return true;
    }
}