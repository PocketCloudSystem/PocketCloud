<?php

namespace pocketcloud\cloud\console\command\argument\def;

use pocketcloud\cloud\console\command\argument\CommandArgument;
use pocketcloud\cloud\console\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;

readonly class ServerArgument extends CommandArgument {

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