<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\CommandParameter;
use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\group\ServerGroupManager;

readonly class ServerGroupParameter extends CommandParameter {

    public function parseValue(string $input): ServerGroup {
        if (($group = ServerGroupManager::getInstance()->get($input)) !== null) return $group;
        throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return array_filter(array_keys(ServerGroupManager::getInstance()->getAll()), fn(string $group) => str_contains(strtolower($group), $currentArg));
    }

    public function getType(): string {
        return "server";
    }
}