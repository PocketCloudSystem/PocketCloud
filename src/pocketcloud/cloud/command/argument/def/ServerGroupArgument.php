<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\CommandArgument;
use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\group\ServerGroup;
use pocketcloud\cloud\group\ServerGroupManager;

final readonly class ServerGroupArgument  extends CommandArgument {

    public function parseValue(string $input): ServerGroup {
        if (($group = ServerGroupManager::getInstance()->get($input)) !== null) return $group;
        throw new ArgumentParseException();
    }

    public function getType(): string {
        return "serverGroup";
    }
}