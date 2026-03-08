<?php

namespace pocketcloud\cloud\scheduler;

use pocketcloud\cloud\exception\TaskCancelException;
use pocketcloud\cloud\plugin\CloudPlugin;
use pocketcloud\cloud\Server;

final class TaskHandler {

    private int $id;
    private int $nextRun;
    private bool $cancelled = false;

    public function __construct(
        private readonly Task $task,
        private readonly int $delay,
        private readonly int $period,
        private readonly bool $repeat,
        private readonly CloudPlugin $owner
    ) {
        $this->id = mt_rand(PHP_INT_MIN, PHP_INT_MAX);
        $this->nextRun = Server::getInstance()->getTick() + $this->delay;
    }

    public function cancel(): void {
        if (!$this->cancelled) {
            $this->cancelled = true;
            $this->task->onCancel();
        }
    }

    public function isCancelled(): bool {
        return $this->cancelled;
    }

    public function onUpdate(int $tick): void {
        if ($tick >= $this->nextRun) {
            $this->nextRun = $tick + $this->period;
            try {
                $this->task->onRun();
            } catch (TaskCancelException) {
                $this->cancel();
            } finally {
                if (!$this->isRepeat()) $this->cancel();
            }
        }
    }

    public function getId(): int {
        return $this->id;
    }

    public function getTask(): Task {
        return $this->task;
    }

    public function getDelay(): int {
        return $this->delay;
    }

    public function getPeriod(): int {
        return $this->period;
    }

    public function isRepeat(): bool {
        return $this->repeat;
    }

    public function getOwner(): CloudPlugin {
        return $this->owner;
    }
}