<?php

namespace pocketcloud\cloud\command;

use pocketcloud\cloud\command\impl\ConfigureCommand;
use pocketcloud\cloud\command\impl\DebugCommand;
use pocketcloud\cloud\command\impl\ExitCommand;
use pocketcloud\cloud\command\impl\group\GroupCommand;
use pocketcloud\cloud\command\impl\HelpCommand;
use pocketcloud\cloud\command\impl\ListCommand;
use pocketcloud\cloud\command\impl\player\KickCommand;
use pocketcloud\cloud\command\impl\plugin\DisableCommand;
use pocketcloud\cloud\command\impl\plugin\EnableCommand;
use pocketcloud\cloud\command\impl\plugin\PluginsCommand;
use pocketcloud\cloud\command\impl\server\ExecuteCommand;
use pocketcloud\cloud\command\impl\server\SaveCommand;
use pocketcloud\cloud\command\impl\server\StartCommand;
use pocketcloud\cloud\command\impl\server\StopCommand;
use pocketcloud\cloud\command\impl\template\CreateCommand;
use pocketcloud\cloud\command\impl\template\EditCommand;
use pocketcloud\cloud\command\impl\template\MaintenanceCommand;
use pocketcloud\cloud\command\impl\template\RemoveCommand;
use pocketcloud\cloud\command\impl\VersionCommand;
use pocketcloud\cloud\command\impl\web\WebAccountCommand;
use pocketcloud\cloud\command\sender\ICommandSender;
use pocketcloud\cloud\util\SingletonTrait;

final class CommandManager {
    use SingletonTrait;

    /** @var array<Command> */
    private array $commands = [];

    public function __construct() {
        self::setInstance($this);
        $this->register(new ExitCommand());
        $this->register(new HelpCommand());
        $this->register(new DebugCommand());
        $this->register(new ListCommand());
        $this->register(new VersionCommand());
        $this->register(new ConfigureCommand());

        $this->register(new StartCommand());
        $this->register(new StopCommand());
        $this->register(new ExecuteCommand());
        $this->register(new SaveCommand());

        $this->register(new CreateCommand());
        $this->register(new EditCommand());
        $this->register(new RemoveCommand());
        $this->register(new MaintenanceCommand());

        $this->register(new KickCommand());

        $this->register(new EnableCommand());
        $this->register(new DisableCommand());
        $this->register(new PluginsCommand());

        $this->register(new WebAccountCommand());

        $this->register(new GroupCommand());
    }

    public function handleInput(ICommandSender $sender, string $input): bool {
        $args = explode(" ", $input);
        $name = array_shift($args);
        if (($command = $this->get($name)) === null) return false;

        $command->handle($sender, $name, $args);
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