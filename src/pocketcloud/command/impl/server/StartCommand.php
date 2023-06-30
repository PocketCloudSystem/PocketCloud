<?php

namespace pocketcloud\command\impl\server;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\language\Language;
use pocketcloud\server\CloudServerManager;
use pocketcloud\template\TemplateManager;

class StartCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        if (isset($args[0])) {
            if (($template = TemplateManager::getInstance()->getTemplateByName($args[0])) !== null) {
                $count = 1;
                if (isset($args[1])) if (is_numeric($args[1])) if (intval($args[1]) > 0) $count = intval($args[1]);

                CloudServerManager::getInstance()->startServer($template, $count);
            } else $sender->error(Language::current()->translate("template.not.found"));
        } else return false;
        return true;
    }
}