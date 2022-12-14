<?php

namespace pocketcloud\command\impl\general;

use pocketcloud\command\Command;
use pocketcloud\command\CommandManager;
use pocketcloud\utils\CloudLogger;

class HelpCommand extends Command {

    public function execute(array $args): bool {
        CloudLogger::get()->info("Commands §8(§e" . count(CommandManager::getInstance()->getCommands()) . "§8)§r:");
        foreach (CommandManager::getInstance()->getCommands() as $command) {
            CloudLogger::get()->info("§e" . $command->getName() . " §8- §e" . $command->getDescription() . " §8- §e" . $command->getUsage() . " §8- §e" . (empty($command->getAliases()) ? "§cNo Aliases" : implode(", ", $command->getAliases())));
        }
        return true;
    }
}