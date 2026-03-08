<?php

namespace pocketcloud\cloud\console;

use Closure;
use pocketcloud\cloud\console\command\ClosureSubCommand;
use pocketcloud\cloud\console\command\Command;
use pocketcloud\cloud\console\command\CommandManager;
use pocketcloud\cloud\console\command\ITabComplete;
use pocketcloud\cloud\console\command\SubCommand;
use pocketcloud\cloud\console\handler\ExceptionHandler;
use pocketcloud\cloud\console\handler\ShutdownHandler;
use pocketcloud\cloud\console\log\color\CloudConsoleColor;
use pocketcloud\cloud\console\screen\ScreenManager;
use pocketcloud\cloud\Server;
use pocketcloud\cloud\setup\Setup;
use pocketcloud\cloud\util\TerminalUtils;
use pocketcloud\cloud\util\trait\SingletonTrait;

/** @internal */
final class Console {
    use SingletonTrait;

    private const string DEFAULT_PROMPT = "§c%s§8@§bcloud §7» §r";
    private const int READLINE_TIMEOUT_MS = 20;

    private ?ManualConsole $manualConsole = null;

    public function __construct() {
        self::setInstance($this);
    }

    public function register(): void {
        ShutdownHandler::register();
        ExceptionHandler::setAll();
    }

    public function install(): void {
        if ($this->manualConsole !== null) return;

        $this->manualConsole = new ManualConsole(
            $this->createDefaultPrompt(),
            $this->defaultCompletionHandler(...),
            fn() => Server::getInstance()->shutdown()
        );
    }

    private function createDefaultPrompt(): string {
        return CloudConsoleColor::toColoredString(
            sprintf(self::DEFAULT_PROMPT, TerminalUtils::getCurrentUser())
        );
    }

    private function defaultCompletionHandler(array $tokens, string $current): array {
        if (empty($tokens)) return $this->completeCommandNames($current);
        return $this->completeCommandArguments($tokens, $current);
    }

    private function completeCommandNames(string $current): array {
        $matches = [];
        $commandManager = CommandManager::getInstance();

        foreach ($commandManager->getAll() as $cmd) {
            if ($this->startsWith($cmd->getName(), $current)) {
                $matches[] = $cmd->getName();
            }

            foreach ($cmd->getAliases() as $alias) {
                if ($this->startsWith($alias, $current)) {
                    $matches[] = $alias;
                }
            }
        }

        foreach (array_keys($commandManager->getKnownStandaloneAliases()) as $standaloneAlias) {
            if ($this->startsWith($standaloneAlias, $current)) {
                $matches[] = $standaloneAlias;
            }
        }

        return $matches;
    }

    private function completeCommandArguments(array $tokens, string $current): array {
        $commandName = array_shift($tokens);
        $command = CommandManager::getInstance()->get($commandName);

        if (!$command instanceof Command) {
            return [];
        }

        $matches = [];
        $originalCommand = $command;
        $actualTokens = $tokens;

        $command = $this->resolveSubCommand($command, $commandName, $tokens, $current, $matches);

        $param = $command->getParameter(count($tokens));
        if ($param !== null) {
            $matches = array_merge($matches, $param->onTabCompleteMatch($current));
        }

        $usedForCustomTabCompletion = $command instanceof ClosureSubCommand ? $originalCommand : $command;
        if ($usedForCustomTabCompletion instanceof ITabComplete) {
            $customMatches = $usedForCustomTabCompletion->onTabComplete(array_merge($actualTokens, [$current]));
            $matches = array_merge($matches, $customMatches);
        }

        return $this->filterMatches(array_unique($matches), $current);
    }

    private function resolveSubCommand(Command $command, string $commandName, array &$tokens, string $current, array &$matches): SubCommand|Command {
        $subCommands = $command->getSubCommands();

        if (empty($subCommands)) return $command;

        $subCommand = $command->getSubCommand($commandName);
        if ($subCommand !== null) return $subCommand;

        if (isset($tokens[0])) {
            $subCommand = $command->getSubCommand($tokens[0]);
            if ($subCommand !== null) {
                array_shift($tokens);
                return $subCommand;
            }
        } else {
            foreach ($subCommands as $subCommand) {
                if ($this->startsWith($subCommand->getName(), $current)) {
                    $matches[] = $subCommand->getName();
                }
            }
        }

        return $command;
    }

    private function filterMatches(array $matches, string $current): array {
        return array_filter(
            $matches,
            fn(string $match) => $this->startsWith(trim($match, "\"'"), $current)
        );
    }

    private function startsWith(string $haystack, string $needle): bool {
        return str_starts_with(strtolower($haystack), strtolower($needle));
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

        var_dump(...$vars);
    }

    public function readLine(): void {
        if (!Server::getInstance()->isRunning()) return;

        $line = $this->manualConsole->readlineNonBlocking(self::READLINE_TIMEOUT_MS);
        $line = trim($line ?? "");

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
        $this->setPrompt(sprintf(self::DEFAULT_PROMPT, TerminalUtils::getCurrentUser()));
    }

    public function getPrompt(): string {
        return $this->manualConsole?->getPrompt() ?? "";
    }

    public function enableHistory(): void {
        $this->manualConsole?->setHistoryEnabled(true);
    }

    public function disableHistory(): void {
        $this->manualConsole?->setHistoryEnabled(false);
    }

    public function enableTyping(): void {
        $this->manualConsole?->setTypingEnabled(true);
    }

    public function disableTyping(): void {
        $this->manualConsole?->setTypingEnabled(false);
    }

    public function showTyping(): void {
        $this->manualConsole?->setVisibleTypingEnabled(true);
    }

    public function hideTyping(): void {
        $this->manualConsole?->setVisibleTypingEnabled(false);
    }

    // Handler Management
    public function setControlCHandler(Closure $handler): void {
        $this->manualConsole?->setControlCHandler($handler);
    }

    public function restoreControlCHandler(): void {
        $this->manualConsole?->setControlCHandler(fn() => Server::getInstance()->shutdown());
    }

    public function setCompletionHandler(Closure $handler): void {
        $this->manualConsole?->setCompletionCallback($handler);
    }

    public function restoreCompletionHandler(): void {
        $this->manualConsole?->setCompletionCallback($this->defaultCompletionHandler(...));
    }

    public function setInput(string $input): void {
        $this->manualConsole?->setInput($input);
    }

    public function getInput(): string {
        return $this->manualConsole?->getInput() ?? "";
    }

    public function remove(): void {
        $this->manualConsole?->close();
        ShutdownHandler::remove();
    }

    public static function getInstance(): ?self {
        return self::$instance;
    }
}