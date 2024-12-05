<?php

namespace pocketcloud\cloud\command\impl;

use pocketcloud\cloud\command\argument\def\StringArgument;
use pocketcloud\cloud\command\Command;
use pocketcloud\cloud\command\CommandManager;
use pocketcloud\cloud\command\sender\ICommandSender;

final class HelpCommand extends Command {

    public function __construct() {
        parent::__construct("help", "List all commands");
        $this->addParameter(new StringArgument(
            "command",
            true
        ));
    }

    public function run(ICommandSender $sender, string $label, array $args): bool {
        $command = $args["command"] ?? null;
        $commands = $command === null ? CommandManager::getInstance()->getAll() : (($tmp = CommandManager::getInstance()->get($command)) === null ? CommandManager::getInstance()->getAll() : [$tmp]);

        foreach ($commands as $command) {
            $sender->info("§b" . $command->getName() . " §8- §r" . $command->getDescription() . " §8- §b" . $command->getUsage());
        }
        return true;
    }
}