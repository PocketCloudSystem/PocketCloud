<?php

namespace pocketcloud\command\impl\template;

use pocketcloud\command\Command;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateManager;
use pocketcloud\template\TemplateType;
use pocketcloud\utils\CloudLogger;

class CreateCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0])) {
            if (!TemplateManager::getInstance()->checkTemplate($args[0])) {
                $templateType = TemplateType::SERVER();
                if (isset($args[1])) $templateType = TemplateType::getTemplateTypeByName($args[1]) ?? TemplateType::SERVER();

                TemplateManager::getInstance()->createTemplate(new Template($args[0], false, true, 20, 0, 2, false, $templateType));
            } else CloudLogger::get()->error("§cThe template already exists!");
        } else return false;
        return true;
    }
}