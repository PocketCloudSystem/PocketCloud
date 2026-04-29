<?php

namespace pocketcloud\cloud\console\command;

use InvalidArgumentException;
use pocketcloud\cloud\console\command\impl\ClearCommand;
use pocketcloud\cloud\console\command\impl\ConfigureCommand;
use pocketcloud\cloud\console\command\impl\DebugCommand;
use pocketcloud\cloud\console\command\impl\ExitCommand;
use pocketcloud\cloud\console\command\impl\group\GroupCommand;
use pocketcloud\cloud\console\command\impl\HelpCommand;
use pocketcloud\cloud\console\command\impl\ListCommand;
use pocketcloud\cloud\console\command\impl\MaintenanceCommand;
use pocketcloud\cloud\console\command\impl\MonitorCommand;
use pocketcloud\cloud\console\command\impl\player\PlayerCommand;
use pocketcloud\cloud\console\command\impl\plugin\PluginCommand;
use pocketcloud\cloud\console\command\impl\server\ServerCommand;
use pocketcloud\cloud\console\command\impl\StatusCommand;
use pocketcloud\cloud\console\command\impl\template\TemplateCommand;
use pocketcloud\cloud\console\command\impl\TimingsCommand;
use pocketcloud\cloud\console\command\impl\VersionCommand;
use pocketcloud\cloud\console\command\sender\ICommandSender;
use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\color\CloudConsoleColor;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\FormatUtils;
use pocketcloud\cloud\util\misc\Loadable;
use pocketcloud\cloud\util\misc\Tickable;
use pocketcloud\cloud\util\promise\Promise;
use pocketcloud\cloud\util\trait\SingletonTrait;

final class CommandManager implements Loadable, Tickable {
    use SingletonTrait;

    /** @var array<Command> */
    private array $commands = [];
    private array $knownAliases = [];
    private array $knownStandaloneAliases = [];
    private array $confirmationPromises = [];
    private ?array $currentConfirmationData = null;

    public function __construct() {
        self::setInstance($this);
    }

    public function load(): void {
        $this->registerAll(
            new ExitCommand(), new HelpCommand(), new ClearCommand(), new MaintenanceCommand(),
            new ConfigureCommand(), new VersionCommand(), new DebugCommand(), new StatusCommand(),
            new MonitorCommand(), new ListCommand(), new TimingsCommand()
        );

        $this->registerAll(new ServerCommand(), new TemplateCommand(), new GroupCommand(), new PlayerCommand(), new PluginCommand());
    }

    public function waitForConfirmation(Command $command, ICommandSender $sender, string $prompt, array $keywordsAccept, int $timeout = 10): Promise {
        $this->confirmationPromises[] = [$command->getName(), $sender, $prompt, PocketCloud::getInstance()->getTick() + (20 * $timeout), $promise = new Promise(), $keywordsAccept];
        return $promise;
    }

    public function register(Command $command): void {
        if (isset($this->commands[$command->getName()])) throw new InvalidArgumentException("The command " . $command->getName() . " is already registered");
        if (array_any($command->getAliases(), fn(string $alias) => in_array(strtolower($alias), $this->knownAliases))) throw new InvalidArgumentException("A command is already using one of the command's aliases");
        $this->commands[strtolower($command->getName())] = $command;
        foreach ($command->getSubCommands() as $subCommand) {
            $subCommand->setParent($command);
            if (count($subCommand->getStandaloneAliases()) > 0) {
                foreach ($subCommand->getStandaloneAliases() as $alias) {
                    if (isset($this->knownStandaloneAliases[strtolower($alias)])) throw new InvalidArgumentException("A standalone alias with the name " . $alias . " is already registered by another command");
                    if (isset($this->commands[strtolower($alias)])) throw new InvalidArgumentException("A command with the same standalone alias name " . $alias . " is already registered");
                    $this->knownStandaloneAliases[strtolower($alias)] = $command->getName() . " " . $subCommand->getName();
                }
            }
        }
    }

    public function registerAll(Command ...$commands): void {
        foreach ($commands as $command) $this->register($command);
    }

    public function remove(Command|string $command): void {
        $command = strtolower($command instanceof Command ? $command->getName() : $command);
        if (isset($this->commands[$command])) unset($this->commands[$command]);
    }

    public function handleInput(ICommandSender $sender, string $name, array $args): bool {
        if ($this->currentConfirmationData !== null) {
            /** @var ICommandSender $sender */
            [, $sender, , , $promise, $keywordsAccept] = $this->currentConfirmationData;
            if (in_array(strtolower($name), $keywordsAccept)) {
                $promise->resolve(true);
            } else {
                $sender->warn("§cCancelled the confirmation.");
                $promise->resolve(false);
            }

            $this->currentConfirmationData = null;
            Console::getInstance()->restorePrompt();
            return true;
        }

        if (isset($this->knownStandaloneAliases[$name])) {
            $replacementParts = explode(" ", $this->knownStandaloneAliases[$name]);
            $name = array_shift($replacementParts);
            array_unshift($args, ...$replacementParts);
        }

        if (($command = $this->get($name)) === null) return false;

        $command->handle($sender, $name, $args);
        return true;
    }

    public function tick(int $currentTick): void {
        if ($this->currentConfirmationData !== null) {
            /** @var ICommandSender $sender */
            [, $sender, , $expireTick, $promise] = $this->currentConfirmationData;
            if ($expireTick <= PocketCloud::getInstance()->getTick()) {
                $this->currentConfirmationData = null;
                $promise->reject();
                $sender->warn("§cConfirmation timed out.");
                Console::getInstance()->restorePrompt();
            }

            return;
        }

        if (!empty($this->confirmationPromises)) {
            $this->currentConfirmationData = array_shift($this->confirmationPromises);
            [, , $prompt, , , $keywordsAccept] = $this->currentConfirmationData;
            $actualPrompt = CloudConsoleColor::toColoredString(trim($prompt) . " §8(" . FormatUtils::interpolate("§rType §8'§a{}§8'§r.", [implode("§8', §8'§a", $keywordsAccept)]) . "§8)§r ");
            Console::getInstance()->setPrompt($actualPrompt);
        }
    }

    private function getStandaloneAliasCommandName(string $standaloneAlias): ?string {
        if (isset($this->knownStandaloneAliases[$standaloneAlias])) {
            $replacementParts = explode(" ", $this->knownStandaloneAliases[$standaloneAlias]);
            return array_shift($replacementParts);
        }

        return null;
    }

    public function get(string $name): ?Command {
        $name = strtolower($name);
        if (($command = $this->getStandaloneAliasCommandName($name)) !== null) return $this->commands[strtolower($command)];
        if (isset($this->commands[$name])) return $this->commands[$name];
        return array_find($this->commands, fn(Command $command) => in_array($name, $command->getAliases()));
    }

    public function getAll(): array {
        return $this->commands;
    }

    public function getKnownAliases(): array {
        return $this->knownAliases;
    }

    public function getKnownStandaloneAliases(): array {
        return $this->knownStandaloneAliases;
    }
}