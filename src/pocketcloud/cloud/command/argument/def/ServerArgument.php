<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\command\argument\CommandArgument;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\server\CloudServerManager;

final readonly class ServerArgument extends CommandArgument {

    public function parseValue(string $input): CloudServer {
        if (($server = CloudServerManager::getInstance()->get($input)) !== null) return $server;
        throw new ArgumentParseException();
    }

    public function getType(): string {
        return "server";
    }
}