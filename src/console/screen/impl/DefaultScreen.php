<?php

namespace pocketcloud\cloud\console\screen\impl;

use pocketcloud\cloud\console\command\CommandManager;
use pocketcloud\cloud\console\command\sender\ConsoleCommandSender;
use pocketcloud\cloud\console\Console;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\console\screen\IScreen;
use pocketcloud\cloud\util\Utils;

final class DefaultScreen extends IScreen {

    public function initialize(Console $console): void {
        $this->restorePrompt();
        $this->restoreCompletionHandler();
        $this->restoreControlCHandler();
    }

    public function tick(int $currentTick): void {}

    public function handleInput(string $input): void {
        $parts = Utils::parseQuoteAware($input);
        if (!CommandManager::getInstance()->handleInput(new ConsoleCommandSender(), $name = array_shift($parts), $parts)) {
            CloudLogger::get()->warn("§cUnknown command §8'§b" . $name . "§r§8'§c. §rView all the commands by doing §8'§bhelp§8'§r.");
        }
    }

    public function onRemove(int $currentTick): void {}
}