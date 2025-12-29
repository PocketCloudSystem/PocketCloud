<?php

namespace pocketcloud\cloud\console\command\argument\def;

use pocketcloud\cloud\console\command\argument\CommandArgument;
use pocketcloud\cloud\console\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\plugin\CloudPluginManager;

readonly class PluginArgument extends CommandArgument {

    public function parseValue(string $input): CloudPlugin {
        if (($plugin = CloudPluginManager::getInstance()->get($input)) !== null) return $plugin;
        throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return array_filter(array_keys(CloudPluginManager::getInstance()->getAll()), fn(string $plugin) => str_contains(strtolower($plugin), $currentArg));
    }

    public function getType(): string {
        return "server";
    }
}