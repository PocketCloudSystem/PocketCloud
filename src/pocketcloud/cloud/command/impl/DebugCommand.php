<?php

namespace pocketcloud\cloud\command\impl;

use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\config\impl\MainConfig;

final class DebugCommand extends Command {

    public function __construct() {
        parent::__construct("debug", "Enable or disable the debug mode");
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        if (MainConfig::getInstance()->isDebugMode()) {
            $sender->info("The §edebug mode §rhas been §cdisabled§r!");
            MainConfig::getInstance()->setDebugMode(false);
        } else {
            $sender->info("The §edebug mode §rhas been §aenabled§r!");
            MainConfig::getInstance()->setDebugMode(true);
        }

        MainConfig::getInstance()->save();
        return true;
    }
}