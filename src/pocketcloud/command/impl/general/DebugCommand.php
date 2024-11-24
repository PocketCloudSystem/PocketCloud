<?php

namespace pocketcloud\command\impl\general;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\config\impl\DefaultConfig;
use pocketcloud\language\Language;

final class DebugCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        if (DefaultConfig::getInstance()->isDebugMode()) {
            $sender->info(Language::current()->translate("debug.disabled"));
            DefaultConfig::getInstance()->setDebugMode(false);
        } else {
            $sender->info(Language::current()->translate("debug.enabled"));
            DefaultConfig::getInstance()->setDebugMode(true);
        }

        DefaultConfig::getInstance()->save();
        return true;
    }
}