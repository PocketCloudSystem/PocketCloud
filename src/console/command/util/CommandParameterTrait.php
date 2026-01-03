<?php

namespace pocketcloud\cloud\console\command\util;

use pocketcloud\cloud\console\command\parameter\CommandParameter;
use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;
use pocketcloud\cloud\console\command\parameter\exception\NoArgumentFoundException;

trait CommandParameterTrait {

    /** @var array<CommandParameter> */
    private array $parameters = [];

    /** @throws ArgumentParseException|NoArgumentFoundException */
    public function parseArgs(array $args, ?CommandParameter &$currentParameter = null): ?array {
        if (count($this->parameters) === 0) return null;
        $parsedArgs = [];
        for ($i = 0; $i < count($this->parameters); $i++) {
            $currentParameter = $this->parameters[$i];
            $multiString = method_exists($currentParameter, "isMultiString") ? $currentParameter->isMultiString() : false;
            if (isset($args[$i])) {
                $arg = $currentParameter->parseValue($multiString ? implode(" ", array_slice($args, $i)) : $args[$i]);
                $parsedArgs[$currentParameter->getName()] = $arg;
                if ($multiString) break;
            } else {
                if ($currentParameter->isOptional()) continue;
                throw new NoArgumentFoundException();
            }
        }

        return $parsedArgs;
    }

    public function addParameter(CommandParameter $argument, ?int $position = null): self {
        if ($position !== null) $this->parameters[$position] = $argument;
        else $this->parameters[] = $argument;
        return $this;
    }

    public function getRequiredParameterCount(): int {
        return count(array_filter($this->parameters, fn(CommandParameter $argument) => !$argument->isOptional()));
    }

    public function getOptionalParameterCount(): int {
        return count(array_filter($this->parameters, fn(CommandParameter $argument) => $argument->isOptional()));
    }

    public function getParameter(int $index): ?CommandParameter {
        return $this->parameters[$index] ?? null;
    }

    public function getParameters(): array {
        return $this->parameters;
    }
}