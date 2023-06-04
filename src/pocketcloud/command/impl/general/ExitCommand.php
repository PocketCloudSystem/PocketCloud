<?php

namespace pocketcloud\command\impl\general;

use pocketcloud\command\Command;
use pocketcloud\language\Language;
use pocketcloud\PocketCloud;
use pocketcloud\util\CloudLogger;

class ExitCommand extends Command {

    public function execute(string $label, array $args): bool {
        if (isset($args[0])) {
            if (strtolower($args[0]) == "confirm") PocketCloud::getInstance()->shutdown();
            else CloudLogger::get()->info(Language::current()->translate("command.exit.confirm"));
        } else CloudLogger::get()->info(Language::current()->translate("command.exit.confirm"));
        return true;
    }
}