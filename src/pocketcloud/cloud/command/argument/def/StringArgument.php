<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\IArgument;

final readonly class StringArgument implements IArgument {

    public function __construct(private string $name) {}

    public function getName(): string {
        return $this->name;
    }

    public function parseValue(string $input): string {
        return $input;
    }
}