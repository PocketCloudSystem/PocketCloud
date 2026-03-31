<?php

namespace pocketcloud\cloud\console\command;

use pocketcloud\cloud\console\command\parameter\exception\ArgumentParseException;
use pocketcloud\cloud\console\command\parameter\exception\FlagParseException;
use pocketcloud\cloud\console\command\parameter\exception\NoArgumentFoundException;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\command\util\CommandFlagTrait;
use pocketcloud\cloud\console\command\util\CommandParameterTrait;
use pocketcloud\cloud\console\log\level\CloudLogLevel;
use pocketcloud\cloud\util\promise\Promise;

abstract class Command {
    use CommandParameterTrait, CommandFlagTrait;

    /** @var array<SubCommand> */
    private array $subCommands = [];

    private bool $useRegularHandlerForSubCommands = false;

    public function __construct(
        private readonly string $name,
        private readonly string $description,
        private readonly array $aliases = [],
        private readonly ?string $usage = null
    ) {}

    final public function waitForConfirmation(ICommandSender $sender, string $prompt, array $keywordsAccept, int $timeout = 10): Promise {
        return CommandManager::getInstance()->waitForConfirmation($this, $sender, $prompt, $keywordsAccept, $timeout);
    }

    public function enableUseRegularHandlerForSubCommands(bool $val = true): self {
        $this->useRegularHandlerForSubCommands = $val;
        return $this;
    }

    /** @internal */
    public function handle(ICommandSender $sender, string $label, array $args): void {
        try {
            ["globalFlags" => $commandGlobalFlags, "regularFlags" => $commandRegularFlags] = $this->scanAndCleanFlags($args);
        } catch (FlagParseException) {
            $this->sendUsageMessage($sender);
            return;
        }

        $subCommand = null;
        if (!empty($this->subCommands)) {
            if ($this->mustUseSubCommands() && count($args) < 1) {
                $this->sendUsageMessage($sender);
                return;
            }

            if (count($args) > 0) $subCommand = $this->getSubCommand($args[0]);
            if ($subCommand === null && $this->mustUseSubCommands()) {
                $this->sendUsageMessage($sender);
                return;
            } else if ($subCommand instanceof SubCommand) array_shift($args);
        }

        $subCommandFlags = [];
        $currentParameter = null;
        try {
            if ($subCommand === null) {
                $parsedArgs = $this->parseArgs($args, $currentParameter);
            } else {
                ["globalFlags" => $_, "regularFlags" => $subCommandFlags] = $subCommand->scanAndCleanFlags($args, true);
                $parsedArgs = $subCommand->parseArgs($args, $currentParameter);
            }
        } catch (FlagParseException|NoArgumentFoundException) {
            $this->sendUsageMessage($sender, $subCommand);
            return;
        } catch (ArgumentParseException) {
            if ($currentParameter?->getCustomErrorMessage() !== null) $sender->warn($currentParameter->getCustomErrorMessage());
            else $this->sendUsageMessage($sender, $subCommand);
            return;
        }

        $finalFlags = array_merge($commandGlobalFlags, $subCommand === null ? $commandRegularFlags : $subCommandFlags);

        if ($subCommand === null) {
            if (!$this->run($sender, $label, $parsedArgs ?? $args, null, $finalFlags)) {
                $this->sendUsageMessage($sender);
            }
        } else {
            if (!($this->useRegularHandlerForSubCommands ? $this->run($sender, $label, $parsedArgs ?? $args, $subCommand, $finalFlags) : $subCommand->run($sender, $label, $parsedArgs ?? $args, $finalFlags))) {
                $this->sendUsageMessage($sender, $subCommand);
            }
        }
    }

    abstract public function run(ICommandSender $sender, string $label, array $args, ?SubCommand $subCommand, array $flags): bool;

    private function buildUsageMessage(?SubCommand $subCommand = null): string {
        if ($subCommand === null) {
            $usage = "";
            $lastSubCommand = array_key_last($this->subCommands);
            foreach ($this->subCommands as $name => $subCommand) {
                $usage .= $subCommand->getUsage();
                if ($lastSubCommand !== $name) $usage .= "\n";
            }

            if ($lastSubCommand !== null && count($this->parameters) > 0) $usage .= "\n";
            if ($usage == "") $usage .= $this->getName();
            foreach ($this->parameters as $parameter) {
                $usage .= $parameter->isOptional() ?
                    " [" . $parameter->getName() . ": " . $parameter->getType() . "]" :
                    " <" . $parameter->getName() . ": " . $parameter->getType() . ">";
            }

            foreach ($this->flags as $flag) {
                $usage .= " " . $flag->buildUsage();
            }
        } else {
            $usage = $subCommand->getUsage();
        }

        return $usage;
    }

    public function sendUsageMessage(ICommandSender $sender, ?SubCommand $subCommand = null, ?CloudLogLevel $logLevel = null): void {
        foreach (explode("\n", $this->getUsage($subCommand)) as $line) {
            $sender->log($logLevel ?? CloudLogLevel::WARN(), trim($line));
        }
    }

    public function registerSubCommand(SubCommand $subCommand): self {
        $this->subCommands[strtolower($subCommand->getName())] = $subCommand;
        return $this;
    }

    public function getSubCommand(string $name): ?SubCommand {
        $name = strtolower($name);
        if (isset($this->subCommands[$name])) return $this->subCommands[$name];
        return array_find($this->subCommands, fn(SubCommand $subCommand) => in_array($name, $subCommand->getStandaloneAliases()));
    }

    public function mustUseSubCommands(): int {
        return count($this->subCommands) > 0 && count($this->parameters) == 0;
    }

    public function getSubCommands(): array {
        return $this->subCommands;
    }

    public function isUseRegularHandlerForSubCommands(): bool {
        return $this->useRegularHandlerForSubCommands;
    }

    public function getName(): string {
        return $this->name;
    }

    public function getDescription(): string {
        return $this->description;
    }

    public function getUsage(?SubCommand $subCommand = null): string {
        if ($subCommand !== null) return $this->buildUsageMessage($subCommand);
        return $this->usage ?? $this->buildUsageMessage();
    }

    public function getAliases(): array {
        return $this->aliases;
    }
}