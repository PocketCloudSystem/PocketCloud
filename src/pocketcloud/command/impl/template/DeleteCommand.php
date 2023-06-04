<?php

namespace pocketcloud\command\impl\template;

use pocketcloud\command\Command;
use pocketcloud\language\Language;
use pocketcloud\template\TemplateManager;
use pocketcloud\util\CloudLogger;

class DeleteCommand extends Command {

    public function execute(string $label, array $args): bool {
        if (isset($args[0])) {
            if (($template = TemplateManager::getInstance()->getTemplateByName($args[0])) !== null) {
                TemplateManager::getInstance()->deleteTemplate($template);
            } else CloudLogger::get()->error(Language::current()->translate("template.not.found"));
        } else return false;
        return true;
    }
}