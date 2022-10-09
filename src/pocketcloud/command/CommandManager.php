<?php

namespace pocketcloud\command;

use pocketcloud\command\impl\general\ExitCommand;
use pocketcloud\command\impl\general\HelpCommand;
use pocketcloud\command\impl\general\ListCommand;
use pocketcloud\command\impl\player\KickCommand;
use pocketcloud\command\impl\plugin\DisableCommand;
use pocketcloud\command\impl\plugin\EnableCommand;
use pocketcloud\command\impl\plugin\PluginsCommand;
use pocketcloud\command\impl\server\ExecuteCommand;
use pocketcloud\command\impl\server\SaveCommand;
use pocketcloud\command\impl\server\StartCommand;
use pocketcloud\command\impl\server\StopCommand;
use pocketcloud\command\impl\template\CreateCommand;
use pocketcloud\command\impl\template\DeleteCommand;
use pocketcloud\command\impl\template\EditCommand;
use pocketcloud\command\impl\template\MaintenanceCommand;
use pocketcloud\event\impl\command\CommandExecuteEvent;
use pocketcloud\event\impl\command\CommandRegisterEvent;
use pocketcloud\event\impl\command\CommandUnregisterCommand;
use pocketcloud\utils\CloudLogger;
use pocketcloud\utils\SingletonTrait;

class CommandManager {
    use SingletonTrait;

    /** @var array<Command> */
    private array $commands = [];

    public function __construct() {
        self::setInstance($this);
        $this->registerCommand(new HelpCommand("help", "Get a list of all commands", "help", ["?"]));
        $this->registerCommand(new PluginsCommand("plugins", "Get a list of all plugins", "plugins", ["pl"]));
        $this->registerCommand(new EnableCommand("enable", "Enable a disabled plugin", "enable <plugin>", []));
        $this->registerCommand(new DisableCommand("disable", "Disable an enabled plugin", "disable <plugin>", []));
        $this->registerCommand(new CreateCommand("create", "Create a template", "create <name> [type (server|proxy): server]", []));
        $this->registerCommand(new DeleteCommand("delete", "Delete a template", "delete <template>", []));
        $this->registerCommand(new MaintenanceCommand("maintenance", "Add/Remove players for the maintenance", "maintenance <add|remove|list> [player]"));
        $this->registerCommand(new EditCommand("edit", "Edit a template", "edit <template> <key> <value>", []));
        $this->registerCommand(new ListCommand("list", "Get a list of all servers, templates & players", "list [servers|templates|players]", []));
        $this->registerCommand(new ExitCommand("exit", "Stop the cloud", "exit", ["end"]));
        $this->registerCommand(new StartCommand("start", "Start a server", "start <template> [count: 1]", []));
        $this->registerCommand(new StopCommand("stop", "Stop a server", "stop <server|template|all>", ["shutdown"]));
        $this->registerCommand(new SaveCommand("save", "Save a server", "save <server>", []));
        $this->registerCommand(new ExecuteCommand("execute", "Send a command to a server", "execute <server> <commandLine>", ["execute"]));
        $this->registerCommand(new KickCommand("kick", "Kick a player", "kick <player> [reason]", []));
    }

    public function registerCommand(Command $command): bool {
        if (!isset($this->commands[$command->getName()])) {
            (new CommandRegisterEvent($command))->call();
            $this->commands[$command->getName()] = $command;
            return true;
        }
        return false;
    }

    public function unregisterCommand(Command $command): bool {
        if (isset($this->commands[$command->getName()])) {
            (new CommandUnregisterCommand($command))->call();
            unset($this->commands[$command->getName()]);
            return true;
        }
        return false;
    }

    public function execute(string $line): void {
        if (trim($line) == "") return;
        $lines = explode(" ", $line);
        $command = $this->getCommandByName($lines[0]);

        if ($command == null) {
            CloudLogger::get()->error("§cThe command doesn't exists!");
            return;
        }

        $args = [];
        $i = 0;
        foreach ($lines as $str) {
            if ($i > 0) {
                $args[] = $str;
            }
            $i++;
        }

        (new CommandExecuteEvent($command))->call();
        if (!$command->execute($args)) CloudLogger::get()->error($command->getUsage());
    }

    public function getCommandByName(string $name): ?Command {
        foreach ($this->commands as $command) {
            if ($command->getName() == strtolower($name) || in_array(strtolower($name), $command->getAliases())) return $command;
        }
        return null;
    }

    /** @return Command[] */
    public function getCommands(): array {
        return $this->commands;
    }
}