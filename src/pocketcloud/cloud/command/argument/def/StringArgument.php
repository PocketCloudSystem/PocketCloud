<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\IArgument;

final readonly class StringArgument implements IArgument {

    public function __construct(
        private string $name,
        private bool $optional
    ) {}

    public function getName(): string {
        return $this->name;
    }

    public function parseValue(string $input): string {
        return $input;
    }

    public function isOptional(): bool {
        return $this->optional;
    }

    public function getType(): string {
        return "string";
    }
}