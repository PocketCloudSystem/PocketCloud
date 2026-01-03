<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\CommandParameter;

readonly class StringParameter extends CommandParameter {

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