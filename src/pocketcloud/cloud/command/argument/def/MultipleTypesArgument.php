<?php

namespace pocketcloud\cloud\command\argument\def;

use pocketcloud\cloud\command\argument\CommandArgument;
use pocketcloud\cloud\command\argument\exception\ArgumentParseException;

final readonly class MultipleTypesArgument extends CommandArgument {

    /**
     * @param string $name
     * @param array<CommandArgument> $allowedTypes
     * @param bool $optional
     */
    public function __construct(
        string $name,
        private array $allowedTypes,
        bool $optional
    ) {
        parent::__construct($name, $optional);
    }

    public function parseValue(string $input): mixed {
        $result = null;
        foreach ($this->allowedTypes as $type) {
            try {
                $result = $type->parseValue($input);
            } catch (ArgumentParseException) {
                continue;
            }
        }

        return $result ?? throw new ArgumentParseException();
    }

    public function getType(): string {
        return implode("|", array_map(fn(CommandArgument $argument) => $argument->getName(), $this->allowedTypes));
    }

    public function getAllowedTypes(): array {
        return $this->allowedTypes;
    }
}