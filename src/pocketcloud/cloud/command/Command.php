<?php

namespace pocketcloud\cloud\command;

use pocketcloud\cloud\command\argument\IArgument;
use pocketcloud\cloud\command\sender\ICommandSender;

abstract class Command {

    private array $parameters = [];

    public function __construct(
        private readonly string $name,
        private readonly string $description,
        private readonly string $usage
    ) {}

    /** @internal */
    public function handle(array $args): void {

    }

    abstract public function run(ICommandSender $sender, string $label, array $args): string;

    public function addParameter(IArgument $argument): void {
        $this->parameters[] = $argument;
    }

    public function getParameters(): array {
        return $this->parameters;
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
}