<?php

namespace pocketcloud\cloud\console\command\util;

use pocketcloud\cloud\console\command\argument\CommandArgument;
use pocketcloud\cloud\console\command\argument\exception\ArgumentParseException;
use pocketcloud\cloud\console\command\argument\exception\NoArgumentFoundException;

trait CommandParameterTrait {

    /** @var array<CommandArgument> */
    private array $parameters = [];

    /** @throws ArgumentParseException|NoArgumentFoundException */
    public function parseArgs(array $args, ?CommandArgument &$currentParameter = null): ?array {
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

    public function addParameter(CommandArgument $argument, ?int $position = null): self {
        if ($position !== null) $this->parameters[$position] = $argument;
        else $this->parameters[] = $argument;
        return $this;
    }

    public function getRequiredParameterCount(): int {
        return count(array_filter($this->parameters, fn(CommandArgument $argument) => !$argument->isOptional()));
    }

    public function getOptionalParameterCount(): int {
        return count(array_filter($this->parameters, fn(CommandArgument $argument) => $argument->isOptional()));
    }

    public function getParameter(int $index): ?CommandArgument {
        return $this->parameters[$index] ?? null;
    }

    public function getParameters(): array {
        return $this->parameters;
    }
}