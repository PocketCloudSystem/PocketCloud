<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\CommandParameter;
use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;

readonly class StringEnumParameter extends CommandParameter {

    protected array $allowedStrings;

    public function __construct(
        string $name,
        array $allowedStrings,
        protected bool $caseSensitive,
        bool $optional,
        protected ?string $typeName = null,
        ?string $customErrorMessage = null
    ) {
        parent::__construct($name, $optional, $customErrorMessage);
        $this->allowedStrings = array_map(function (string $string): string {
            return $this->caseSensitive ? $string : strtolower($string);
        }, $allowedStrings);
    }

    public function parseValue(string $input): string {
        $input = $this->caseSensitive ? $input : strtolower($input);
        if (!$this->isAllowedString($input)) throw new ArgumentParseException("Given string is not allowed inside this StringEnumArgument");
        return $input;
    }

    public function onTabCompleteMatch(string $currentArg): array {
        if ($currentArg == "") return $this->allowedStrings;
        return array_values(array_filter(
            $this->allowedStrings,
            fn (string $string) => str_contains($string, $currentArg)
        ));
    }

    public function getType(): string {
        if ($this->typeName !== null) return $this->typeName;
        $allowedStrings = $this->allowedStrings;
        if (count($allowedStrings) > 2) {
            return implode("|", array_slice($allowedStrings, 0, 2)) . "|...";
        }

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
