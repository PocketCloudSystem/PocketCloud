<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\CommandArgument;

final readonly class MixedArgument extends CommandArgument {

    public function parseValue(string $input): string {
        return $input;
    }

    public function getType(): string {
        return "mixed";
    }
}