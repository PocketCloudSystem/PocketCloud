<?php

namespace pocketcloud\cloud\terminal;

use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\command\CommandManager;
use pocketcloud\cloud\command\sender\ConsoleCommandSender;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\thread\Thread;
use pocketmine\snooze\SleeperHandlerEntry;

final class Terminal extends Thread {

    private ThreadSafeArray $buffer;
    private SleeperHandlerEntry $entry;

    public function __construct() {
        $this->buffer = new ThreadSafeArray();

        $this->entry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function (): void {
            while (($line = $this->buffer->shift()) !== null) {
                if (trim($line) == "") return;
                if (!CommandManager::getInstance()->handleInput(new ConsoleCommandSender(), $line)) {
                    CloudLogger::get()->error("The §bcommand §rdoesn't exists!");
                }
            }
        });
    }

    public function onRun(): void {
        $input = fopen("php://stdin", "r");
        while ($this->isRunning()) {
            $this->buffer[] = trim(fgets($input));
            $this->entry->createNotifier()->wakeupSleeper();
        }

        fclose($input);
    }
}