<?php

namespace pocketcloud\cloud\console\command\parameter\def;

use pocketcloud\cloud\console\command\parameter\CommandParameter;
use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;

readonly class MultipleTypesParameter extends CommandParameter {

    /**
     * @param string $name
     * @param array<CommandParameter> $allowedTypes
     * @param bool $optional
     * @param string|null $customErrorMessage
     */
    public function __construct(
        string $name,
        protected array $allowedTypes,
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
        return implode("|", array_map(fn(CommandParameter $argument) => $argument->getName(), $this->allowedTypes));
    }

    public function getAllowedTypes(): array {
        return $this->allowedTypes;
    }
}