<?php

namespace pocketcloud\command\impl\template;

use pocketcloud\command\Command;
use pocketcloud\language\Language;
use pocketcloud\template\TemplateManager;
use pocketcloud\util\CloudLogger;

class EditCommand extends Command {

    public function execute(string $label, array $args): bool {
        if (isset($args[0]) && isset($args[1]) && isset($args[2])) {
            if (($template = TemplateManager::getInstance()->getTemplateByName($args[0])) !== null) {
                if ($template::isValidEditKey($args[1])) {
                    if ($template::isValidEditValue($args[2], $args[1], $expected, $realValue)) {
                        TemplateManager::getInstance()->editTemplate(
                            $template,
                            ($args[1] == "lobby" ? $realValue : null),
                            ($args[1] == "maintenance" ? $realValue : null),
                            ($args[1] == "static" ? $realValue : null),
                            ($args[1] == "maxPlayerCount" ? $realValue : null),
                            ($args[1] == "minServerCount" ? $realValue : null),
                            ($args[1] == "maxServerCount" ? $realValue : null),
                            ($args[1] == "startNewWhenFull" ? $realValue : null),
                            ($args[1] == "autoStart" ? $realValue : null),
                        );
                    } else CloudLogger::get()->error(Language::current()->translate("command.edit.failed.second", $args[1], $expected));
                } else CloudLogger::get()->error(Language::current()->translate("command.edit.failed.first"));
            } else CloudLogger::get()->error(Language::current()->translate("template.not.found"));
        } else return false;
        return true;
    }
}