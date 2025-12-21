<?php

namespace pocketcloud\cloud\console\command\argument\def;

use pocketcloud\cloud\console\command\argument\CommandArgument;

readonly class BoolArgument extends CommandArgument {

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