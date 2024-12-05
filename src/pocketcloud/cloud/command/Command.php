<?php

namespace pocketcloud\cloud\command;

use pocketcloud\cloud\command\argument\def\StringArgument;
use pocketcloud\cloud\command\argument\def\StringEnumArgument;
use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\command\argument\CommandArgument;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\terminal\log\CloudLogger;

abstract class Command {

    /** @var array<CommandArgument> */
    private array $parameters = [];

    public function __construct(
        private readonly string $name,
        private readonly string $description,
        private readonly ?string $usage = null
    ) {}

    /** @internal */
    public function handle(ICommandSender $sender, string $label, array $args): void {
        if (empty($this->parameters)) {
            $this->run($sender, $label, $args);
            return;
        }

        $parsedArgs = [];
        for ($i = 0; $i < count($this->parameters); $i++) {
            $currentParameter = $this->parameters[$i];
            $multiString = $currentParameter instanceof StringArgument && $currentParameter->isMultiString();
            if (isset($args[$i])) {
                try {
                    if ($currentParameter instanceof StringEnumArgument && $currentParameter->isAllowedString($args[$i])) {
                        CloudLogger::get()->warn($currentParameter->getCustomErrorMessage() ?? $this->getUsage());
                        return;
                    }

                    $arg = $currentParameter->parseValue($multiString ? array_slice($args, $i) : $args[$i]);
                    $parsedArgs[$currentParameter->getName()] = $arg;
                    if ($multiString) break;
                } catch (ArgumentParseException) {
                    CloudLogger::get()->warn($currentParameter->getCustomErrorMessage() ?? $this->getUsage());
                    return;
                }
            } else {
                if ($currentParameter->isOptional()) continue;
                CloudLogger::get()->warn($this->getUsage());
                return;
            }
        }

        if (!$this->run($sender, $label, $parsedArgs)) {
            CloudLogger::get()->warn($this->getUsage());
        }
    }

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

    public function addParameter(CommandArgument $argument): void {
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
        return $this->usage ?? $this->buildUsageMessage();
    }
}