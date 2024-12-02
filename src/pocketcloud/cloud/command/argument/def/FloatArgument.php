<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\command\argument\IArgument;

final readonly class FloatArgument implements IArgument {

    public function __construct(private string $name) {}

    public function getName(): string {
        return $this->name;
    }

    public function parseValue(string $input): int {
        if (is_numeric($input)) return floatval($input);
        return throw new ArgumentParseException();
    }
}