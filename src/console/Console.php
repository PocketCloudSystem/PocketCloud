<?php

namespace pocketcloud\cloud\console;

use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\CommandManager;
use pocketcloud\cloud\console\command\ITabComplete;
use pocketcloud\cloud\console\command\sender\ConsoleCommandSender;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\handler\ShutdownHandler;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\util\TerminalUtils;
use pocketcloud\cloud\util\trait\SingletonTrait;
use pocketcloud\cloud\console\log\color\CloudConsoleColor;
use pocketcloud\cloud\util\Utils;

final class Console {
    use SingletonTrait;

    private ?ManualConsole $manualConsole = null;

    public function __construct() {
        self::setInstance($this);
        $this->install();
    }

    public function register(): void {
        ShutdownHandler::register();
        ExceptionHandler::setAll();
    }

    public function install(): void {
        $this->manualConsole = new ManualConsole(
            CloudConsoleColor::toColoredString("§c" . TerminalUtils::getCurrentUser() . "§8@§bcloud §7» §r"),
            function(array $tokens, string $current): array {
                $matches = [];
                if (empty($tokens)) {
                    foreach (CommandManager::getInstance()->getAll() as $cmd) {
                        if (str_starts_with($cmd->getName(), strtolower($current))) {
                            $matches[] = $cmd->getName();
                        } else {
                            foreach ($cmd->getAliases() as $alias) {
                                if (str_starts_with($alias, strtolower($current))) {
                                    $matches[] = $alias;
                                }
                            }
                        }
                    }
                    return $matches;
                }

                $command = CommandManager::getInstance()->get(array_shift($tokens));

                if ($command instanceof ITabComplete) {
                    $matches = $command->onTabComplete(array_merge($tokens, [$current]));
                } else if ($command instanceof Command) {
                    $subCommands = $command->getSubCommands();
                    if (count($subCommands) > 0) {
                        if (isset($tokens[0])) {
                            if (($subCommand = $command->getSubCommand($tokens[0])) !== null) {
                                array_shift($tokens);
                                $command = $subCommand;
                            }
                        } else {
                            foreach ($subCommands as $subCommand) {
                                if (str_starts_with($subCommand->getName(), strtolower($current))) {
                                    $matches[] = $subCommand->getName();
                                } else {
                                    foreach ($subCommand->getAliases() as $alias) {
                                        if (str_starts_with($alias, strtolower($current))) {
                                            $matches[] = $alias;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    $param = $command->getParameter(count($tokens));
                    if ($param !== null) {
                        $matches = array_unique(array_merge($matches, $param->onTabCompleteMatch($current)));
                    }
                }

                return array_filter($matches, fn(string $match) => str_starts_with(trim($match, "\"'"), $current));
            },
            fn() => PocketCloud::getInstance()->shutdown()
        );
    }

    public function println(string $message): void {
        $this->manualConsole?->println($message);
    }

    public function dump(mixed ...$vars): void {
        $this->manualConsole?->dump(...$vars);
    }

    public function readLine(): void {
        if (!PocketCloud::getInstance()->isRunning()) return;
        $line = trim($this->manualConsole->readlineNonBlocking(timeoutMs: 50) ?? "");
        if ($line === "") return;
        $parts = Utils::parseQuoteAware($line);

        if (!CommandManager::getInstance()->handleInput(new ConsoleCommandSender(), $name = array_shift($parts), $parts)) {
            CloudLogger::get()->warn("§cUnknown command §8'§b" . $name . "§r§8'§c. §rView all the commands by doing §8'§bhelp§8'§r.");
        }
    }

    public function remove(): void {
        $this->manualConsole->close();
        ShutdownHandler::remove();
    }

    public static function getInstance(): ?self {
        return self::$instance;
    }
}