<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\CommandArgument;

final readonly class StringEnumArgument extends CommandArgument {

    private array $allowedStrings;

    public function __construct(
        string $name,
        array $allowedStrings,
        private bool $caseSensitive,
        bool $optional,
        ?string $customErrorMessage = null
    ) {
        parent::__construct($name, $optional, $customErrorMessage);
        $this->allowedStrings = array_map(fn(string $string) => $this->caseSensitive ? $string : strtolower($string), $allowedStrings);
    }

    public function parseValue(string $input): string {
        return $this->caseSensitive ? $input : strtolower($input);
    }

    public function getType(): string {
        return implode("|", $this->allowedStrings);
    }

    public function getAllowedStrings(): array {
        return $this->allowedStrings;
    }

    public function isAllowedString(string $string): bool {
        return in_array($this->caseSensitive ? $string : strtolower($string), $this->allowedStrings);
    }

    public function isCaseSensitive(): bool {
        return $this->caseSensitive;
    }
}
