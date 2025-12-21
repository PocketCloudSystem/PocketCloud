<?php

namespace pocketcloud\cloud\console\command;

use pocketcloud\cloud\console\command\impl\ExitCommand;
use pocketcloud\cloud\console\command\impl\HelpCommand;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\util\misc\Loadable;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class CommandManager implements Loadable {
    use SingletonTrait;

    /** @var array<Command> */
    private array $commands = [];

    public function __construct() {
        self::setInstance($this);
    }

    public function load(): void {
        $this->register(new ExitCommand());
        $this->register(new HelpCommand());
    }

    public function handleInput(ICommandSender $sender, string $name, array $args): bool {
        if (($command = $this->get($name)) === null) return false;

        $command->handle($sender, $name, $args);
        return true;
    }

    public function register(Command $command): void {
        $this->commands[strtolower($command->getName())] = $command;
    }

    public function remove(Command|string $command): void {
        $command = strtolower($command instanceof Command ? $command->getName() : $command);
        if (isset($this->commands[$command])) unset($this->commands[$command]);
    }

    public function get(string $name): ?Command {
        $name = strtolower($name);
        if (isset($this->commands[$name])) return $this->commands[$name];
        return array_find($this->commands, fn(Command $command) => in_array($name, $command->getAliases()));
    }

    public function getAll(): array {
        return $this->commands;
    }
}