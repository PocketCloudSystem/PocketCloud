<?php

namespace pocketcloud\cloud\scheduler;

use pocketcloud\cloud\thread\Worker;
use pocketmine\snooze\SleeperHandlerEntry;
use pocketmine\snooze\SleeperNotifier;
use pocketmine\utils\AssumptionFailedError;

final class AsyncWorker extends Worker {

    private static ?SleeperNotifier $notifier = null;

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

        self::$notifier = $this->entry->createNotifier();
    }

    public static function getNotifier(): SleeperNotifier {
        if (self::$notifier !== null) return self::$notifier;
        throw new AssumptionFailedError("SleeperNotifier not found in thread-local storage");
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
