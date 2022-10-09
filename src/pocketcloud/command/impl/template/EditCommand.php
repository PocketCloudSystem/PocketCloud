<?php

namespace pocketcloud\command\impl\template;

use pocketcloud\command\Command;
use pocketcloud\template\TemplateManager;
use pocketcloud\utils\CloudLogger;

class EditCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0]) && isset($args[1]) && isset($args[2])) {
            if (($template = TemplateManager::getInstance()->getTemplateByName($args[0])) !== null) {
                if ($template::isValidEditKey($args[1])) {
                    if ($template::isValidEditValue($args[2], $args[1], $expected, $realValue)) {
                        TemplateManager::getInstance()->editTemplate(
                            $template,
                            ($args[1] == "lobby" ? $realValue : null),
                            ($args[1] == "maintenance" ? $realValue : null),
                            ($args[1] == "maxPlayerCount" ? $realValue : null),
                            ($args[1] == "minServerCount" ? $realValue : null),
                            ($args[1] == "maxServerCount" ? $realValue : null),
                            ($args[1] == "autoStart" ? $realValue : null),
                        );
                    } else CloudLogger::get()->error("§cYou've provided the wrong value for the key §e" . $args[1] . "§c! Expected: §e" . $expected);
                } else CloudLogger::get()->error("§cThe edit key doesn't exists! §8(§rValid keys: §elobby, maintenance, maxPlayerCount, minServerCount, maxServerCount, autoStart§8)");
            } else CloudLogger::get()->error("§cThe template doesn't exists!");
        } else return false;
        return true;
    }
}