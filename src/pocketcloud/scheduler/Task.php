<?php

namespace pocketcloud\scheduler;

abstract class Task {

    private ?TaskHandler $taskHandler = null;

    abstract public function onRun(): void;

    public function onCancel(): void {}

    public function setTaskHandler(TaskHandler $taskHandler): void {
        $this->taskHandler = $taskHandler;
    }

    public function getTaskHandler(): TaskHandler {
        return $this->taskHandler;
    }
}