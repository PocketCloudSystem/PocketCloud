<?php

namespace pocketcloud\console;

use pocketcloud\command\CommandManager;
use pocketcloud\lib\snooze\SleeperNotifier;
use pocketcloud\PocketCloud;
use pocketcloud\thread\Thread;

class Console extends Thread {

    private \Threaded $buffer;
    private SleeperNotifier $notifier;

    public function __construct() {
        $this->buffer = new \Threaded();
        $this->notifier = new SleeperNotifier();

        PocketCloud::getInstance()->getSleeperHandler()->addNotifier($this->notifier, function (): void {
            while (($line = $this->buffer->shift()) !== null) CommandManager::getInstance()->execute($line);
        });
    }

    public function run() {
        while ($this->isRunning()) {
            $input = fopen("php://stdin", "r");
            $line = trim(fgets($input));
            fclose($input);

            if ($line !== "") {
                $this->buffer[] = $line;
                $this->notifier->wakeupSleeper();
            }
        }
    }
}