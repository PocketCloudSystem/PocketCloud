<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\command\argument\IArgument;

final readonly class IntegerArgument implements IArgument {

    public function __construct(
        private string $name,
        private bool $optional
    ) {}

    public function getName(): string {
        return $this->name;
    }

    public function parseValue(string $input): int {
        if (is_numeric($input)) return intval($input);
        return throw new ArgumentParseException();
    }

    public function isOptional(): bool {
        return $this->optional;
    }

    public function getType(): string {
        return "integer";
    }
}