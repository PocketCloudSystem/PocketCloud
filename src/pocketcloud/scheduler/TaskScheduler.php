<?php

namespace pocketcloud\scheduler;

use pocketcloud\utils\SingletonTrait;

class TaskScheduler {
    use SingletonTrait;

    /** @var array<TaskHandler> */
    private array $tasks = [];
    private int $tick = 0;

    private function scheduleTask(Task $task, int $delay, int $period, bool $repeat): void {
        $taskHandler = new TaskHandler($task, $delay, $period, $repeat);
        $task->setTaskHandler($taskHandler);
        $this->tasks[$taskHandler->getId()] = $taskHandler;
    }

    public function scheduleDelayedTask(Task $task, int $delay) {
        $this->scheduleTask($task, $delay, -1, false);
    }

    public function scheduleRepeatingTask(Task $task, int $period) {
        $this->scheduleTask($task, -1, $period, true);
    }

    public function scheduleDelayedRepeatingTask(Task $task, int $delay, int $period) {
        $this->scheduleTask($task, $delay, $period, true);
    }

    public function cancel(Task $task) {
        if (isset($this->tasks[$task->getTaskHandler()->getId()])) {
            $task->getTaskHandler()->cancel();
            unset($this->tasks[$task->getTaskHandler()->getId()]);
        }
    }

    public function cancelAll() {
        foreach ($this->tasks as $task) $task->cancel();
        $this->tasks = [];
    }

    public function getTaskById(int $id): ?Task {
        foreach ($this->tasks as $task) if ($task->getId() == $id) return $task->getTask();
        return null;
    }

    public function onUpdate() {
        $this->tick++;

        foreach ($this->tasks as $id => $handler) {
            if ($handler->isCancelled()) {
                unset($this->tasks[$id]);
                continue;
            }
            $handler->onUpdate($this->tick);
        }
    }

    public function getTasks(): array {
        return $this->tasks;
    }
}