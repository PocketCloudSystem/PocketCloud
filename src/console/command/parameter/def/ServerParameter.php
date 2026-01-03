<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\CommandParameter;
use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;

readonly class ServerParameter extends CommandParameter {

    public function parseValue(string $input): CloudServer {
        if (($server = CloudServerManager::getInstance()->get($input)) !== null) return $server;
        throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return array_filter(array_keys(CloudServerManager::getInstance()->getAll()), fn(string $server) => str_contains(strtolower($server), $currentArg));
    }

    public function getType(): string {
        return "server";
    }
}