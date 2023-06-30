<?php

namespace pocketcloud\command;

use pocketcloud\command\sender\ICommandSender;

abstract class Command {

    public function __construct(
        private readonly string $name,
        private string $description,
        private readonly string $usage,
        private readonly array $aliases = []
    ) {}

    abstract public function execute(ICommandSender $sender, string $label, array $args): bool;

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