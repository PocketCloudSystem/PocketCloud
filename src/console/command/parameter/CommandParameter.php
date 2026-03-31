<?php

namespace pocketcloud\cloud\console\command\parameter;

use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;

abstract readonly class CommandParameter {

    public function __construct(
        protected string $name,
        protected bool $optional,
        protected ?string $customErrorMessage = null
    ) {}

    /** @throws ArgumentParseException */
    abstract public function parseValue(string $input): mixed;

    public function onTabCompleteMatch(string $currentArg): array {
        return [];
    }

    abstract public function getType(): string;

    public function getName(): string {
        return $this->name;
    }

    public function isOptional(): bool {
        return $this->optional;
    }

    public function getCustomErrorMessage(): ?string {
        return $this->customErrorMessage;
    }
}