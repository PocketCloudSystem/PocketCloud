<?php

namespace pocketcloud\scheduler;

abstract class Task {

    private ?TaskHandler $taskHandler = null;

    public function onRun() {}

    public function onCancel() {}

    public function setTaskHandler(TaskHandler $taskHandler): void {
        $this->taskHandler = $taskHandler;
    }

    public function getTaskHandler(): TaskHandler {
        return $this->taskHandler;
    }
}