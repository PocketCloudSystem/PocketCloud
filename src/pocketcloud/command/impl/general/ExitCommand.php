<?php

namespace pocketcloud\command\impl\general;

use pocketcloud\command\Command;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\language\Language;
use pocketcloud\PocketCloud;

class ExitCommand extends Command {

    public function execute(ICommandSender $sender, string $label, array $args): bool {
        if (isset($args[0])) {
            if (strtolower($args[0]) == "confirm") PocketCloud::getInstance()->shutdown();
            else $sender->info(Language::current()->translate("command.exit.confirm"));
        } else $sender->info(Language::current()->translate("command.exit.confirm"));
        return true;
    }
}