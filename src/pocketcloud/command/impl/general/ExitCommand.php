<?php

namespace pocketcloud\command\impl\general;

use pocketcloud\command\Command;
use pocketcloud\PocketCloud;
use pocketcloud\utils\CloudLogger;

class ExitCommand extends Command {

    public function execute(array $args): bool {
        if (isset($args[0])) {
            if (strtolower($args[0]) == "confirm") PocketCloud::getInstance()->shutdown();
            else CloudLogger::get()->info("Are you sure you want to stop the cloud? If yes confirm it with \"§eexit confirm§r\"!");
        } else CloudLogger::get()->info("Are you sure you want to stop the cloud? If yes confirm it with \"§eexit confirm§r\"!");
        return true;
    }
}