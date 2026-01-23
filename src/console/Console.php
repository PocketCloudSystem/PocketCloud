<?php

namespace pocketcloud\cloud\console;

use Closure;
use pocketcloud\cloud\console\command\ClosureSubCommand;
use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\CommandManager;
use pocketcloud\cloud\console\command\ITabComplete;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\handler\ShutdownHandler;
use pocketcloud\cloud\console\screen\ScreenManager;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\setup\Setup;
use pocketcloud\cloud\util\TerminalUtils;
use pocketcloud\cloud\util\trait\SingletonTrait;
use pocketcloud\cloud\console\log\color\CloudConsoleColor;

/** @internal */
final class Console {
    use SingletonTrait;

    private ?ManualConsole $manualConsole = null;

    public function __construct() {
        self::setInstance($this);
    }

    public function register(): void {
        ShutdownHandler::register();
        ExceptionHandler::setAll();
    }

    public function install(): void {
        if ($this->manualConsole === null) $this->manualConsole = new ManualConsole(
            CloudConsoleColor::toColoredString("§c" . TerminalUtils::getCurrentUser() . "§8@§bcloud §7» §r"),
            $this->defaultCompletionHandler(...),
            fn() => PocketCloud::getInstance()->shutdown()
        );
    }

    private function defaultCompletionHandler(array $tokens, string $current): array {
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

            foreach (array_keys(CommandManager::getInstance()->getKnownStandaloneAliases()) as $standaloneAlias) {
                if (str_starts_with($standaloneAlias, strtolower($current))) {
                    $matches[] = $standaloneAlias;
                }
            }
            return $matches;
        }

        $command = CommandManager::getInstance()->get(array_shift($tokens));

        if ($command instanceof Command) {
            $originalCommand = $command;
            $subCommands = $command->getSubCommands();
            $actualTokens = $tokens;
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
                        }
                    }
                }
            }

            $param = $command->getParameter(count($tokens));
            if ($param !== null) {
                $matches = array_unique(array_merge($matches, $param->onTabCompleteMatch($current)));
            }

            $usedForCustomTabCompletion = $command instanceof ClosureSubCommand ? $originalCommand : $command;
            if ($usedForCustomTabCompletion instanceof ITabComplete) {
                $matches = array_unique(array_merge($matches, $usedForCustomTabCompletion->onTabComplete(array_merge($actualTokens, [$current]))));
            }
        }

        return array_filter($matches, fn(string $match) => str_starts_with(trim($match, "\"'"), $current));
    }

    public function println(string $message): void {
        if ($this->manualConsole !== null) {
            $this->manualConsole->println($message);
            return;
        }

        echo $message . PHP_EOL;
    }

    public function dump(mixed ...$vars): void {
        if ($this->manualConsole !== null) {
            $this->manualConsole->dump(...$vars);
            return;
        }

        var_dump($vars);
    }

    public function readLine(): void {
        if (!PocketCloud::getInstance()->isRunning()) return;
        $line = trim($this->manualConsole->readlineNonBlocking(timeoutMs: 50) ?? "");
        if ($line === "") {
            if ($this->manualConsole->isPressedEnter()) Setup::getCurrentSetup()?->handleInput($line);
            return;
        }

        ScreenManager::getInstance()->getCurrentScreen()->handleInput($line);
    }

    public function setPrompt(string $prompt): void {
        $this->manualConsole?->setPrompt(CloudConsoleColor::toColoredString($prompt));
    }

    public function restorePrompt(): void {
        $this->setPrompt("§c" . TerminalUtils::getCurrentUser() . "§8@§bcloud §7» §r");
    }

    public function enableHistory(): void {
        $this->manualConsole?->setAddToHistory(true);
    }

    public function disableHistory(): void {
        $this->manualConsole?->setAddToHistory(false);
    }

    public function restoreControlCHandler(): void {
        $this->manualConsole?->setControlCHandler(fn() => PocketCloud::getInstance()->shutdown());
    }

    public function setControlCHandler(Closure $handler): void {
        $this->manualConsole?->setControlCHandler($handler);
    }

    public function restoreCompletionHandler(): void {
        $this->manualConsole?->setCompletionCallback($this->defaultCompletionHandler(...));
    }

    public function setCompletionHandler(Closure $handler): void {
        $this->manualConsole?->setCompletionCallback($handler);
    }

    public function setInput(string $input): void {
        $this->manualConsole?->setInput($input);
    }

    public function remove(): void {
        $this->manualConsole?->close();
        ShutdownHandler::remove();
    }

    public static function getInstance(): ?self {
        return self::$instance;
    }
}