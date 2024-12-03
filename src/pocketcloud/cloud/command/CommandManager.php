<?php

namespace pocketcloud\cloud\command;

use pocketcloud\cloud\command\impl\ExitCommand;
use pocketcloud\cloud\command\impl\HelpCommand;
use pocketcloud\cloud\util\SingletonTrait;

final class CommandManager {
    use SingletonTrait;

    /** @var array<Command> */
    private array $commands = [];

    public function __construct() {
        self::setInstance($this);
        $this->register(new ExitCommand());
        $this->register(new HelpCommand());
    }

    public function handleInput(string $input): bool {
        $args = explode(" ", $input);
        $name = array_shift($args);
        if (($command = $this->get($name)) === null) return false;

        $command->handle($name, $args);
        return true;
    }

    public function register(Command $command): void {
        $this->commands[$command->getName()] = $command;
    }

    public function remove(Command|string $command): void {
        $command = $command instanceof Command ? $command->getName() : $command;
        if (isset($this->commands[$command])) unset($this->commands[$command]);
    }

    public function get(string $name): ?Command {
        return $this->commands[$name] ?? null;
    }

    public function getAll(): array {
        return $this->commands;
    }
}