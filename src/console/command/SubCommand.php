<?php

namespace pocketcloud\cloud\console\command;

use Closure;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\util\CommandParameterTrait;

abstract class SubCommand {
    use CommandParameterTrait;

    public function __construct(
        private readonly string $name,
        private readonly ?string $description = null,
        private readonly ?string $usage = null,
        private readonly array $aliases = []
    ) {}

    abstract public function run(ICommandSender $sender, string $label, array $args): bool;

    private function buildUsageMessage(): string {
        $usage = $this->getName();
        foreach ($this->parameters as $parameter) {
            $usage .= $parameter->isOptional() ?
                " [" . $parameter->getName() . ": " . $parameter->getType() . "]" :
                " <" . $parameter->getName() . ": " . $parameter->getType() . ">";
        }

        return $usage;
    }

    public function getName(): string {
        return $this->name;
    }

    public function getDescription(): ?string {
        return $this->description;
    }

    public function getUsage(): string {
        return $this->usage ?? $this->buildUsageMessage();
    }

    public function getAliases(): array {
        return $this->aliases;
    }

    /**
     * @param string $name
     * @param Closure $executeHandler (function (ICommandSender $sender, string $label, array $args): bool {})
     * @param string|null $description
     * @param string|null $usage
     * @param array|null $aliases
     * @return ClosureSubCommand
     */
    public static function fromClosure(string $name, Closure $executeHandler, ?string $description = null, ?string $usage = null, ?array $aliases = []): ClosureSubCommand {
        return new ClosureSubCommand($name, $executeHandler, $description, $usage, $aliases);
    }

    public static function nonHandler(string $name, ?string $description = null, ?string $usage = null, ?array $aliases = []): ClosureSubCommand {
        return new ClosureSubCommand($name, null, $description, $usage, $aliases);
    }
}