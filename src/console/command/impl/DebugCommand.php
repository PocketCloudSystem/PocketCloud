<?php

namespace pocketcloud\cloud\console\command\impl;

use pocketcloud\cloud\config\impl\LogSettingsConfig;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\console\handler\ExceptionHandler;

final class DebugCommand extends Command {

    public function __construct() {
        parent::__construct("debug", "Enable or disable the debug mode", ["deb"]);
    }

    public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand = null): bool {
        if (LogSettingsConfig::getInstance()->isDebugMode()) {
            $sender->success("The §edebug mode §rhas been §cdisabled§r!");
            LogSettingsConfig::getInstance()->setDebugMode(false);
        } else {
            $sender->success("The §edebug mode §rhas been §aenabled§r!");
            LogSettingsConfig::getInstance()->setDebugMode(true);
        }

        ExceptionHandler::tryCatch(fn() => MainConfig::getInstance()->save(), "Failed to save main config");
        return true;
    }
}