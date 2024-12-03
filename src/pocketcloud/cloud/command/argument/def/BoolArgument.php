<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\IArgument;

final readonly class BoolArgument implements IArgument {

    public function __construct(
        private string $name,
        private bool $optional
    ) {}

    public function getName(): string {
        return $this->name;
    }

    public function parseValue(string $input): bool {
        if (strtolower($input) == "true" || strtolower($input) == "yes") return true;
        return false;
    }

    public function isOptional(): bool {
        return $this->optional;
    }

    public function getType(): string {
        return "boolean";
    }
}