<?php

namespace pocketcloud\scheduler;

use pocketcloud\utils\SingletonTrait;

class AsyncPool {
    use SingletonTrait;

    /** @var array<AsyncTask> */
    private array $asyncTasks = [];
    private int $tick = 0;

    public function submitTask(AsyncTask $asyncTask) {
        $this->asyncTasks[] = $asyncTask;
        $asyncTask->start();
    }

    public function onUpdate() {
        $this->tick++;

        foreach ($this->asyncTasks as $index => $task) {
            if ($task->isDone()) {
                $task->onCompletion();
                unset($this->asyncTasks[$index]);
            }
        }
    }

    public function getAsyncTasks(): array {
        return $this->asyncTasks;
    }
}