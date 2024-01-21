<?php

namespace pocketcloud\console;

use pmmp\thread\ThreadSafeArray;
use pocketcloud\command\CommandManager;
use pocketcloud\PocketCloud;
use pocketcloud\setup\Setup;
use pocketcloud\thread\Thread;
use pocketcloud\util\Utils;
use pocketmine\snooze\SleeperHandlerEntry;

class Console extends Thread {

    private ThreadSafeArray $buffer;
    private SleeperHandlerEntry $entry;

    public function __construct() {
        $this->buffer = new ThreadSafeArray();

        $this->entry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function (): void {
            while (($line = $this->buffer->shift()) !== null) if (!PocketCloud::getInstance()->isReloading()) {
                if (($setup = Setup::getCurrentSetup()) === null) {
                    CommandManager::getInstance()->execute($line);
                } else {
                    $setup->handleInput($line);
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