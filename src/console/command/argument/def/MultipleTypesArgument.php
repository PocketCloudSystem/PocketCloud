<?php

namespace pocketcloud\cloud\console\command\argument\def;

use pocketcloud\cloud\console\command\argument\CommandArgument;
use pocketcloud\cloud\console\command\argument\exception\ArgumentParseException;

readonly class MultipleTypesArgument extends CommandArgument {

    /**
     * @param string $name
     * @param array<CommandArgument> $allowedTypes
     * @param bool $optional
     * @param string|null $customErrorMessage
     */
    public function __construct(
        string $name,
        private array $allowedTypes,
        bool $optional,
        ?string $customErrorMessage = null
    ) {
        parent::__construct($name, $optional, $customErrorMessage);
    }

    public function parseValue(string $input): mixed {
        $result = null;
        foreach ($this->allowedTypes as $type) {
            try {
                $result = $type->parseValue($input);
                break;
            } catch (ArgumentParseException) {
                continue;
            }
        }

        return $result ?? throw new ArgumentParseException();
    }

    public function onTabCompleteMatch(string $currentArg): array {
        $matches = [];
        foreach ($this->allowedTypes as $type) {
            $matches = array_merge($type->onTabCompleteMatch($currentArg), $matches);
        }

        return array_unique(array_values($matches));
    }

    public function getType(): string {
        return implode("|", array_map(fn(CommandArgument $argument) => $argument->getName(), $this->allowedTypes));
    }

    public function getAllowedTypes(): array {
        return $this->allowedTypes;
    }
}