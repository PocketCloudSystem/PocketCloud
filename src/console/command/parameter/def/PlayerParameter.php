<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\CommandParameter;
use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;

readonly class PlayerParameter extends CommandParameter {

    public function parseValue(string $input): CloudPlayer {
        if (($player = CloudPlayerManager::getInstance()->get($input)) !== null) return $player;
        throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return array_filter(array_keys(CloudPlayerManager::getInstance()->getAll()), fn(string $player) => str_contains(strtolower($player), $currentArg));
    }

    public function getType(): string {
        return "player";
    }
}