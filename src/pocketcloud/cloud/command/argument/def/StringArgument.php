<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\CommandArgument;

final readonly class StringArgument extends CommandArgument {

    public function __construct(
        string $name,
        bool $optional,
        private bool $multiString = false
    ) {
        parent::__construct($name, $optional);
    }

    public function parseValue(string $input): string {
        return $input;
    }

    public function getType(): string {
        return "string";
    }

    public function isMultiString(): bool {
        return $this->multiString;
    }
}