<?php

namespace pocketcloud\scheduler;

use pocketcloud\plugin\CloudPlugin;

class TaskHandler {

    private int $id;
    private bool $cancelled = false;
    private int $last = 0;

    public function __construct(
        private Task $task,
        private int $delay,
        private int $period,
        private bool $repeat,
        private CloudPlugin $owner
    ) {
        $this->id = mt_rand(PHP_INT_MIN, PHP_INT_MAX);
    }

    public function cancel() {
        if (!$this->cancelled) {
            $this->cancelled = true;
            $this->task->onCancel();
        }
    }

    public function isCancelled(): bool {
        return $this->cancelled;
    }

    public function onUpdate(int $tick) {
        if ($this->delay > 0) {
            if (--$this->delay == 0) {
                $this->last = $tick;
                $this->task->onRun();
                if (!$this->isRepeat()) $this->cancel();
            }
            return;
        }

        if ($tick >= ($this->last + $this->period)) {
            $this->last = $tick;
            $this->task->onRun();
            if (!$this->isRepeat()) $this->cancel();
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