<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\CommandParameter;

readonly class BoolParameter extends CommandParameter {

    public function __construct(string $name, bool $optional, ?string $customErrorMessage = null) {
        parent::__construct($name, $optional, $customErrorMessage);
    }

    public function parseValue(string $input): bool {
        if (strtolower($input) == "true" || strtolower($input) == "yes") return true;
        return false;
    }

    public function onTabCompleteMatch(string $currentArg): array {
        return ["true", "false"];
    }

    public function getType(): string {
        return "boolean";
    }
}