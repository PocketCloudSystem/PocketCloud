<?php

namespace pocketcloud\cloud\console\screen;

use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\util\TerminalUtils;

abstract class IScreen {

    final public function enableHistory(): void {
        Console::getInstance()->enableHistory();
    }

    final public function disableHistory(): void {
        Console::getInstance()->disableHistory();
    }

    final public function enableTyping(): void {
        Console::getInstance()->enableTyping();
    }

    final public function disableTyping(): void {
        Console::getInstance()->disableTyping();
    }

    final public function showTyping(): void {
        Console::getInstance()->showTyping();
    }

    final public function hideTyping(): void {
        Console::getInstance()->hideTyping();
    }

    final public function clear(): void {
        TerminalUtils::clear();
    }

    abstract public function initialize(Console $console): void;

    abstract public function handleInput(string $input): void;

    abstract public function tick(int $currentTick): void;

    abstract public function onRemove(int $currentTick): void;
}