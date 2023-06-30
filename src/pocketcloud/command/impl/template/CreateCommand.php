<?php

namespace pocketcloud\command\impl\template;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\language\Language;
use pocketcloud\setup\impl\TemplateSetup;
use pocketcloud\template\Template;
use pocketcloud\template\TemplateManager;
use pocketcloud\template\TemplateType;

class CreateCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        if (isset($args[0])) {
            if (strtolower($args[0]) == "setup") {
                (new TemplateSetup())->startSetup();
            } else {
                if (!TemplateManager::getInstance()->checkTemplate($args[0])) {
                    $templateType = TemplateType::SERVER();
                    if (isset($args[1])) $templateType = TemplateType::getTemplateTypeByName($args[1]) ?? TemplateType::SERVER();

                    TemplateManager::getInstance()->createTemplate(new Template($args[0], false, true, false, 20, 0, 2, false, false, $templateType));
                } else $sender->error(Language::current()->translate("template.already.exists"));
            }
        } else (new TemplateSetup())->startSetup();
        return true;
    }
}