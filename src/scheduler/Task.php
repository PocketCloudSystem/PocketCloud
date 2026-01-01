<?php

namespace pocketcloud\cloud\scheduler;

use pocketcloud\cloud\exception\TaskCancelException;

abstract class Task {

    private ?TaskHandler $taskHandler = null;

    /** @throws TaskCancelException */
    abstract public function onRun(): void;

    public function onCancel(): void {}

    public function setTaskHandler(TaskHandler $taskHandler): void {
        $this->taskHandler = $taskHandler;
    }

    public function getTaskHandler(): TaskHandler {
        return $this->taskHandler;
    }
}