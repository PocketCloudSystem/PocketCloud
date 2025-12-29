<?php

namespace pocketcloud\cloud\console\command\argument\def;

use pocketcloud\cloud\console\command\argument\CommandArgument;
use pocketcloud\cloud\console\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\group\ServerGroupManager;

readonly class ServerGroupArgument extends CommandArgument {

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