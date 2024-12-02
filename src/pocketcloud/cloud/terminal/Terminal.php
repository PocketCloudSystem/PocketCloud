<?php

namespace pocketcloud\cloud\terminal;

use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\PocketCloud;
use pocketcloud\cloud\thread\Thread;
use pocketmine\snooze\SleeperHandlerEntry;

final class Terminal extends Thread {

    private ThreadSafeArray $buffer;
    private SleeperHandlerEntry $entry;

    public function __construct() {
        $this->buffer = new ThreadSafeArray();

        $this->entry = PocketCloud::getInstance()->getSleeperHandler()->addNotifier(function (): void {
            while (($line = $this->buffer->shift()) !== null) {
                //TODO: handle command input
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