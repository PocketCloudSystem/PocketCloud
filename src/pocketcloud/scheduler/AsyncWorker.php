<?php

namespace pocketcloud\scheduler;

use pocketcloud\thread\Worker;
use pocketmine\snooze\SleeperHandlerEntry;

class AsyncWorker extends Worker {

    public function __construct(
        private readonly int $id,
        private readonly int $memoryLimit,
        private readonly SleeperHandlerEntry $entry
    ) {}

    public function onRun(): void {
        gc_enable();
        if ($this->memoryLimit > 0) {
            ini_set("memory_limit", $this->memoryLimit . "M");
        } else {
            ini_set("memory_limit", "-1");
        }
    }

    public function getThreadName() : string{
        return "AsyncWorker#" . $this->id;
    }

    public function getAsyncWorkerId() : int{
        return $this->id;
    }

    public function getEntry(): SleeperHandlerEntry {
        return $this->entry;
    }
}
