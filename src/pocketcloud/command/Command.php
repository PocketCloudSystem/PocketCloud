<?php

namespace pocketcloud\command;

abstract class Command {

    public function __construct(
        private string $name,
        private string $description,
        private string $usage,
        private array $aliases = []
    ) {}

    abstract public function execute(string $label, array $args): bool;

    public function setDescription(string $description): void {
        $this->description = $description;
    }

    public function getName(): string {
        return $this->name;
    }

    public function getDescription(): string {
        return $this->description;
    }

    public function getUsage(): string {
        return $this->usage;
    }

    public function getAliases(): array {
        return $this->aliases;
    }
}