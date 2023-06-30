<?php

namespace pocketcloud\command\impl\template;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\language\Language;
use pocketcloud\template\TemplateManager;

class DeleteCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        if (isset($args[0])) {
            if (($template = TemplateManager::getInstance()->getTemplateByName($args[0])) !== null) {
                TemplateManager::getInstance()->deleteTemplate($template);
            } else $sender->error(Language::current()->translate("template.not.found"));
        } else return false;
        return true;
    }
}