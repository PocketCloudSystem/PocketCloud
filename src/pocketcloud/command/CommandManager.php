<?php

namespace pocketcloud\command;

use pocketcloud\command\impl\general\ExitCommand;
use pocketcloud\command\impl\general\HelpCommand;
use pocketcloud\command\impl\general\ListCommand;
use pocketcloud\command\impl\general\ReloadCommand;
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
use pocketcloud\command\impl\web\WebAccountCommand;
use pocketcloud\command\sender\ConsoleCommandSender;
use pocketcloud\command\sender\ICommandSender;
use pocketcloud\event\impl\command\CommandExecuteEvent;
use pocketcloud\event\impl\command\CommandRegisterEvent;
use pocketcloud\event\impl\command\CommandUnregisterCommand;
use pocketcloud\language\Language;
use pocketcloud\util\CloudLogger;
use pocketcloud\util\Reloadable;
use pocketcloud\util\SingletonTrait;

class CommandManager implements Reloadable {
    use SingletonTrait;

    /** @var array<string, Command> */
    private array $commands = [];
    /** @var array<string, Command> */
    private array $knownAliases = [];

    public function __construct() {
        self::setInstance($this);
        $this->registerCommand(new HelpCommand("help", "command.description.help", "help", ["?"]));
        $this->registerCommand(new PluginsCommand("plugins", "command.description.plugins", "plugins", ["pl"]));
        $this->registerCommand(new EnableCommand("enable", "command.description.enable", "enable <plugin>", []));
        $this->registerCommand(new DisableCommand("disable", "command.description.disable", "disable <plugin>", []));
        $this->registerCommand(new CreateCommand("create", "command.description.create", "create <name|setup> [type (server|proxy): server]", []));
        $this->registerCommand(new DeleteCommand("delete", "command.description.delete", "delete <template>", []));
        $this->registerCommand(new MaintenanceCommand("maintenance", "command.description.maintenance", "maintenance <add|remove|list> [player]"));
        $this->registerCommand(new EditCommand("edit", "command.description.edit", "edit <template> <key> <value>", []));
        $this->registerCommand(new ListCommand("list", "command.description.list", "list [servers|templates|players]", []));
        $this->registerCommand(new ExitCommand("exit", "command.description.exit", "exit", ["end"]));
        $this->registerCommand(new StartCommand("start", "command.description.start", "start <template> [count: 1]", []));
        $this->registerCommand(new StopCommand("stop", "command.description.stop", "stop <server|template|all>", ["shutdown"]));
        $this->registerCommand(new SaveCommand("save", "command.description.save", "save <server>", []));
        $this->registerCommand(new ExecuteCommand("execute", "command.description.execute", "execute <server> <commandLine>", ["execute"]));
        $this->registerCommand(new KickCommand("kick", "command.description.kick", "kick <player> [reason]", []));
        $this->registerCommand(new ReloadCommand("reload", "command.description.reload", "reload", []));
        $this->registerCommand(new WebAccountCommand("webaccount", "command.description.webaccount", "webaccount <create|remove|list|update>", ["webacc"]));
    }

    public function execute(string $line, ?ICommandSender $sender = null): void {
        if (trim($line) == "") return;
        $sender = $sender ?? new ConsoleCommandSender();
        $args = explode(" ", $line);
        $command = $this->getCommand($label = array_shift($args));

        if ($command == null) {
            CloudLogger::get()->error(Language::current()->translate("command.not.found"));
            return;
        }

        (new CommandExecuteEvent($sender, $command))->call();
        if (!$command->execute($sender, $label, $args)) CloudLogger::get()->error($command->getUsage());
    }

    public function registerCommand(Command $command): void {
        if (!isset($this->commands[strtolower($command->getName())])) {
            (new CommandRegisterEvent($command))->call();
            $this->commands[strtolower($command->getName())] = $command;
            if (count($command->getAliases()) > 0) {
                foreach ($command->getAliases() as $alias) $this->knownAliases[strtolower($alias)] = $command;
            }
        }
    }

    public function unregisterCommand(Command|string $command): void {
        $command = $command instanceof Command ? strtolower($command->getName()) : strtolower($command);
        if (isset($this->commands[$command])) {
            (new CommandUnregisterCommand($commandClass = $this->commands[$command]))->call();
            unset($this->commands[$command]);
            foreach ($commandClass->getAliases() as $alias) {
                if (isset($this->knownAliases[strtolower($alias)])) unset($this->knownAliases[strtolower($alias)]);
            }
        }
    }

    public function reload(): bool {
        foreach ($this->commands as $command) {
            if ($command->getDescription() == "command.description." . $command->getName()) {
                $command->setDescription(Language::current()->translate($command->getDescription()));
            }
        }
        return true;
    }

    public function getCommand(string $name): ?Command {
        return $this->commands[strtolower($name)] ?? $this->knownAliases[strtolower($name)] ?? null;
    }

    public function getCommands(): array {
        return $this->commands;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}