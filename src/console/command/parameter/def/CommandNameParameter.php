<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\CommandParameter;
use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;
use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\CommandManager;

readonly class CommandNameParameter extends CommandParameter {

    public function parseValue(string $input): Command {
        if (($command = CommandManager::getInstance()->get($input))) return $command;
        return throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return array_keys(CommandManager::getInstance()->getAll());
    }

    public function getType(): string {
        return "command";
    }
}