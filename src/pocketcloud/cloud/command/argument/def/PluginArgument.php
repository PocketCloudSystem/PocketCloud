<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\CommandArgument;
use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\plugin\CloudPluginManager;

final readonly class PluginArgument extends CommandArgument {

    public function parseValue(string $input): CloudPlugin {
        if (($plugin = CloudPluginManager::getInstance()->get($input)) !== null) return $plugin;
        throw new ArgumentParseException();
    }

    public function getType(): string {
        return "plugin";
    }
}