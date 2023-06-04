<?php

namespace pocketcloud\scheduler;

use JetBrains\PhpStorm\Pure;
use pocketcloud\plugin\CloudPlugin;
use pocketcloud\util\Tickable;

class TaskScheduler implements Tickable {

    /** @var array<TaskHandler> */
    private array $tasks = [];

    public function __construct(private CloudPlugin $owner) {}

    private function scheduleTask(Task $task, int $delay, int $period, bool $repeat): void {
        $taskHandler = new TaskHandler($task, $delay, $period, $repeat, $this->owner);
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

    #[Pure] public function getTaskById(int $id): ?Task {
        foreach ($this->tasks as $task) if ($task->getId() == $id) return $task->getTask();
        return null;
    }

    public function tick(int $currentTick): void {
        foreach ($this->tasks as $id => $task) {
            if ($task->isCancelled()) {
                unset($this->tasks[$id]);
                continue;
            }
            $task->onUpdate($currentTick);
        }
    }

    public function getTasks(): array {
        return $this->tasks;
    }

    public function getOwner(): CloudPlugin {
        return $this->owner;
    }
}