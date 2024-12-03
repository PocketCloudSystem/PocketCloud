<?php

namespace pocketcloud\cloud\command;

use pocketcloud\cloud\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\command\argument\IArgument;
use pocketcloud\cloud\terminal\log\CloudLogger;

abstract class Command {

    /** @var array<IArgument> */
    private array $parameters = [];

    public function __construct(
        private readonly string $name,
        private readonly string $description,
        private readonly ?string $usage = null
    ) {}

    /** @internal */
    public function handle(string $label, array $args): void {
        if (empty($this->parameters)) {
            $this->run($label, $args);
            return;
        }

        $parsedArgs = [];
        for ($i = 0; $i < count($this->parameters); $i++) {
            $currentParameter = $this->parameters[$i];
            if (isset($args[$i])) {
                try {
                    $arg = $currentParameter->parseValue($args[$i]);
                    $parsedArgs[$currentParameter->getName()] = $arg;
                } catch (ArgumentParseException) {
                    CloudLogger::get()->warn($this->getUsage());
                    break;
                }
            } else {
                if ($currentParameter->isOptional()) continue;
                break;
            }
        }

        $this->run($label, $parsedArgs);
    }

    abstract public function run(string $label, array $args): string;

    private function buildUsageMessage(): string {
        $usage = $this->getName();
        foreach ($this->parameters as $parameter) {
            $usage .= $parameter->isOptional() ?
                " [" . $parameter->getName() . ": " . $parameter->getType() . "]" :
                " <" . $parameter->getName() . ": " . $parameter->getType() . ">";
        }

        return $usage;
    }

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
        return $this->usage ?? $this->buildUsageMessage();
    }
}