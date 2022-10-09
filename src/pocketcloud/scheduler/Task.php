<?php

namespace pocketcloud\scheduler;

abstract class Task {

    private ?TaskHandler $taskHandler = null;

    public function onRun() {}

    public function onCancel() {}

    public function setTaskHandler(TaskHandler $taskHandler) {
        $this->taskHandler = $taskHandler;
    }

    public function getTaskHandler(): TaskHandler {
        return $this->taskHandler;
    }
}