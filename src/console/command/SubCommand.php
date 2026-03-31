<?php

namespace pocketcloud\cloud\console\command;

use Closure;
use pocketcloud\cloud\console\command\flag\CommandFlag;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\util\CommandFlagTrait;
use pocketcloud\cloud\console\command\util\CommandParameterTrait;

abstract class SubCommand {
    use CommandParameterTrait, CommandFlagTrait;

    private ?Command $parent = null;

    public function __construct(
        private readonly string $name,
        private readonly array $standaloneAliases = [],
        private readonly ?string $usage = null
    ) {}

    abstract public function run(ICommandSender $sender, string $label, array $args, array $flags): bool;

    private function buildUsageMessage(): string {
        $usage = ($this->parent?->getName() ?? "<parent command>") . " " . $this->getName();
        foreach ($this->parameters as $parameter) {
            $usage .= $parameter->isOptional() ?
                " [" . $parameter->getName() . ": " . $parameter->getType() . "]" :
                " <" . $parameter->getName() . ": " . $parameter->getType() . ">";
        }

        if (count($this->parent?->getFlags() ?? []) > 0) {
            /** @var CommandFlag $flag */
            foreach ($this->parent?->getFlags() ?? [] as $flag) {
                if ($flag->isGlobal()) {
                    $usage .= " " . $flag->buildUsage();
                }
            }
        }

        foreach ($this->flags as $flag) {
            $usage .= " " . $flag->buildUsage();
        }

        return $usage;
    }

    /** @internal */
    public function setParent(?Command $parent): void {
        $this->parent = $parent;
    }

    public function getName(): string {
        return $this->name;
    }

    /**
     * These are aliases that are treated the same way as regular commands are, making it possible to shorten the actual command.
     * Example: The command 'server' has a subcommand called 'stop'. When we now set the standaloneAliases from the 'stop' subcommand to 'stop',
     * you can directly execute the 'stop' sub-command without typing the main-command ('server') first,
     * resulting in a much nicer pocketcloud usage.
     * @return array
     */
    public function getStandaloneAliases(): array {
        return $this->standaloneAliases;
    }

    public function getUsage(): string {
        return $this->usage ?? $this->buildUsageMessage();
    }

    /**
     * @param string $name
     * @param Closure(ICommandSender $sender, string $label, array $args, array $flags): bool $executeHandler
     * @param array|null $standaloneAliases
     * @param string|null $usage
     * @return ClosureSubCommand
     */
    public static function fromClosure(string $name, Closure $executeHandler, ?array $standaloneAliases = [], ?string $usage = null): ClosureSubCommand {
        return new ClosureSubCommand($name, $executeHandler, $standaloneAliases, $usage);
    }

    public static function nonHandler(string $name, ?array $standaloneAliases = [], ?string $usage = null): ClosureSubCommand {
        return new ClosureSubCommand($name, null, $standaloneAliases, $usage);
    }
}