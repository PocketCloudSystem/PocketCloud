<?php

namespace pocketcloud\cloud\command\impl;

use pocketcloud\cloud\command\argument\def\StringArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\CommandManager;
use pocketcloud\cloud\terminal\log\CloudLogger;

final class HelpCommand extends Command {

    public function __construct() {
        parent::__construct("help", "List all commands");
        $this->addParameter(new StringArgument(
            "command",
            true
        ));
    }

    public function run(string $label, array $args): bool {
        $command = $args["command"] ?? null;
        $commands = $command === null ? CommandManager::getInstance()->getAll() : (($tmp = CommandManager::getInstance()->get($command)) === null ? CommandManager::getInstance()->getAll() : [$tmp]);

        foreach ($commands as $command) {
            CloudLogger::get()->info("§e" . $command->getName() . " §8- §e" . $command->getDescription() . " §8- §e" . $command->getUsage());
        }
        return true;
    }
}