<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;
use pocketcloud\cloud\console\command\parameter\CommandParameter;

readonly class FloatParameter extends CommandParameter {

    public function parseValue(string $input): int {
        if (is_numeric($input)) return floatval($input);
        return throw new ArgumentParseException();
    }

    public function getType(): string {
        return "float";
    }
}