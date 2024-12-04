<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\command\argument\CommandArgument;
use pocketcloud\cloud\player\CloudPlayer;
use pocketcloud\cloud\player\CloudPlayerManager;

final readonly class PlayerArgument extends CommandArgument {

    public function parseValue(string $input): CloudPlayer {
        if (($player = CloudPlayerManager::getInstance()->get($input)) !== null) return $player;
        throw new ArgumentParseException();
    }

    public function getType(): string {
        return "player";
    }
}