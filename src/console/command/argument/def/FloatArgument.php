<?php

namespace pocketcloud\cloud\console\command\argument\def;

use pocketcloud\cloud\console\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\console\command\argument\CommandArgument;

readonly class FloatArgument extends CommandArgument {

    public function parseValue(string $input): int {
        if (is_numeric($input)) return floatval($input);
        return throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return [];
    }

    public function getType(): string {
        return "float";
    }
}