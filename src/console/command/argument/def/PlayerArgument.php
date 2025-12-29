<?php

namespace pocketcloud\cloud\console\command\argument\def;

use pocketcloud\cloud\console\command\argument\CommandArgument;
use pocketcloud\cloud\console\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;

readonly class PlayerArgument extends CommandArgument {

    public function parseValue(string $input): CloudPlayer {
        if (($player = CloudPlayerManager::getInstance()->get($input)) !== null) return $player;
        throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return array_filter(array_keys(CloudPlayerManager::getInstance()->getAll()), fn(string $player) => str_contains(strtolower($player), $currentArg));
    }

    public function getType(): string {
        return "server";
    }
}