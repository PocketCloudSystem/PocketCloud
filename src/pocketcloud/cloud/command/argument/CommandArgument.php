<?php

namespace pocketcloud\cloud\command\argument;

use pocketcloud\cloud\command\argument\exception\ArgumentParseException;

abstract readonly class CommandArgument {

    public function __construct(
        private string $name,
        private bool $optional,
        private ?string $customErrorMessage = null
    ) {}

    /** @throws ArgumentParseException */
    abstract public function parseValue(string $input): mixed;

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