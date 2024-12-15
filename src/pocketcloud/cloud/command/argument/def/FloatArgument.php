<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\command\argument\CommandArgument;

final readonly class FloatArgument extends CommandArgument {

    public function parseValue(string $input): int {
        if (is_numeric($input)) return floatval($input);
        return throw new ArgumentParseException();
    }

    public function getType(): string {
        return "float";
    }
}