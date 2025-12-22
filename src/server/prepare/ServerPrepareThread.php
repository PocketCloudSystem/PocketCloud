<?php

namespace pocketcloud\cloud\server\prepare;

use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\thread\Thread;
use pocketmine\snooze\SleeperHandlerEntry;

/** @internal */
final class ServerPrepareThread extends Thread {

    private SleeperHandlerEntry $sleeperHandlerEntry;
    /** @var ThreadSafeArray<ServerPrepareEntry> */
    private ThreadSafeArray $prepareQueue;
    /** @var ThreadSafeArray<ServerPrepareEntry> */
    private ThreadSafeArray $finishedPreparations;

    public function __construct() {
        $this->prepareQueue = new ThreadSafeArray();
        $this->finishedPreparations = new ThreadSafeArray();
    }

    public function onRun(): void {
        while ($this->isRunning()) {
            $this->synchronized(function (): void {
                if ($this->isRunning() &&
                    $this->prepareQueue->count() == 0 &&
                    $this->finishedPreparations->count() == 0) $this->wait();
            });

            /** @var ServerPrepareEntry $entry */
            if (($entry = $this->prepareQueue->shift()) !== null) {
                $entry->run();
                $this->finishedPreparations[] = $entry;
                $this->sleeperHandlerEntry->createNotifier()->wakeupSleeper();
            }
        }
    }

    public function setSleeperHandlerEntry(SleeperHandlerEntry $sleeperHandlerEntry): void {
        $this->sleeperHandlerEntry = $sleeperHandlerEntry;
    }

    public function pushToQueue(ServerPrepareEntry $entry): void {
        $this->synchronized(function () use ($entry): void {
            $this->prepareQueue[] = $entry;
            $this->notify();
        });
    }

    public function getPrepareQueue(): ThreadSafeArray {
        return $this->prepareQueue;
    }

    public function getFinishedPreparations(): ThreadSafeArray {
        return $this->finishedPreparations;
    }
}