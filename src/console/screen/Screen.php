<?php

namespace pocketcloud\cloud\console\screen;

use Closure;
use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\logger\cache\LogMessagesCache;
use pocketcloud\cloud\console\log\output\OutputHandler;
use pocketcloud\cloud\console\log\output\OutputManager;
use pocketcloud\cloud\util\TerminalUtils;

abstract class Screen {

    abstract public function initialize(Console $console): void;

    abstract public function handleInput(string $input): void;

    abstract public function tick(int $currentTick): void;

    abstract public function onRemove(int $currentTick): void;

    final public function printLogCache(): void {
        LogMessagesCache::print();
    }

    final public function enableHistory(): void {
        Console::getInstance()->enableHistory();
    }

    final public function enableTyping(): void {
        Console::getInstance()->enableTyping();
    }

    final public function showTyping(): void {
        Console::getInstance()->showTyping();
    }

    final public function showCursor(): void {
        TerminalUtils::showCursor();
    }

    final public function disableHistory(): void {
        Console::getInstance()->disableHistory();
    }

    final public function disableTyping(): void {
        Console::getInstance()->disableTyping();
    }

    final public function hideTyping(): void {
        Console::getInstance()->hideTyping();
    }

    final public function hideCursor(): void {
        TerminalUtils::hideCursor();
    }

    final public function clearConsole(): void {
        TerminalUtils::clearConsole();
    }

    final public function setOutputHandler(OutputHandler $handler): void {
        OutputManager::setHandler($handler);
    }

    final public function setPrompt(string $prompt): void {
        Console::getInstance()->setPrompt($prompt);
    }
    
    final public function setInput(string $input): void {
        Console::getInstance()->setInput($input);
    }

    final public function setControlCHandler(Closure $handler): void {
        Console::getInstance()->setControlCHandler($handler);
    }

    final public function setCompletionHandler(Closure $handler): void {
        Console::getInstance()->setCompletionHandler($handler);
    }

    final public function restoreAll(): void {
        $this->restoreControlCHandler();
        $this->restoreCompletionHandler();
        $this->restorePrompt();
    }

    final public function restorePrompt(): void {
        Console::getInstance()->restorePrompt();
    }

    final public function restoreControlCHandler(): void {
        Console::getInstance()->restoreControlCHandler();
    }

    final public function restoreCompletionHandler(): void {
        Console::getInstance()->restoreCompletionHandler();
    }

    final public function resetOutputManager(): void {
        OutputManager::reset();
    }

    final public function getPrompt(): string {
        return Console::getInstance()->getPrompt();
    }

    final public function getInput(): string {
        return Console::getInstance()->getInput();
    }
}