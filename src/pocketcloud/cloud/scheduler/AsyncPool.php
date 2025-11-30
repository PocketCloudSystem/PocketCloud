<?php

namespace pocketcloud\cloud\scheduler;

use pmmp\thread\Thread;
use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\util\SingletonTrait;
use pocketcloud\cloud\util\tick\Tickable;
use pocketmine\snooze\SleeperHandler;
use SplQueue;

final class AsyncPool implements Tickable {
    use SingletonTrait;

    private int $size = 10;
    private SleeperHandler $eventLoop;

    /** @var array<int, SplQueue<AsyncTask>> */
    private array $taskQueues = [];
    /** @var array<AsyncWorker> */
    private array $workers = [];
    /** @var array<int, int> */
    private array $workerLastUsed = [];

    public function __construct() {
        self::setInstance($this);
        $this->eventLoop = new SleeperHandler();
    }

    public function increaseSize(int $newSize): void {
        if ($newSize > $this->size) {
            $this->size = $newSize;
        }
    }

    public function getSize(): int {
        return $this->size;
    }

    public function getRunningWorkers(): array {
        return array_keys($this->workers);
    }

    private function getWorker(int $worker): AsyncWorker {
        if (!isset($this->workers[$worker])) {
            $entry = $this->eventLoop->addNotifier(fn() => $this->collectTasksFromWorker($worker));
            $this->workers[$worker] = $asyncWorker = new AsyncWorker($worker, MainConfig::getInstance()->getMemoryLimit(), $entry);
            $asyncWorker->start(Thread::INHERIT_INI);
            $this->taskQueues[$worker] = new SplQueue();
        }

        return $this->workers[$worker];
    }

    public function submitTaskToWorker(AsyncTask $task, int $worker): void {
        if ($worker < 0 || $worker >= $this->size) return;
        if($task->isSubmitted()) return;
        $task->setSubmitted(true);

        $this->getWorker($worker)->stack($task);
        $this->taskQueues[$worker]->enqueue($task);
        $this->workerLastUsed[$worker] = time();
    }

    public function selectWorker(): int {
        $worker = null;
        $minUsage = PHP_INT_MAX;
        foreach ($this->taskQueues as $i => $queue) {
            if (($usage = $queue->count()) < $minUsage) {
                $worker = $i;
                $minUsage = $usage;
                if ($usage === 0) break;
            }
        }

        if ($worker === null || ($minUsage > 0 && count($this->workers) < $this->size)) {
            for ($i = 0; $i < $this->size; ++$i) {
                if (!isset($this->workers[$i])) {
                    $worker = $i;
                    break;
                }
            }
        }

        assert($worker !== null);
        return $worker;
    }

    public function submitTask(AsyncTask $task): int {
        if($task->isSubmitted()) return -1;

        $worker = $this->selectWorker();
        $this->submitTaskToWorker($task, $worker);
        return $worker;
    }

    public function collectTasks(): bool {
        foreach ($this->taskQueues as $worker => $queue) $this->collectTasksFromWorker($worker);
        return array_any($this->taskQueues, fn($queue) => !$queue->isEmpty());
    }

    public function collectTasksFromWorker(int $worker): bool {
        if (!isset($this->taskQueues[$worker])) return false;
        $queue = $this->taskQueues[$worker];
        $more = false;
        while (!$queue->isEmpty()) {
            /** @var AsyncTask $task */
            $task = $queue->bottom();
            if ($task->isDone()) {
                $queue->dequeue();
                $task->onCompletion();
            } else {
                $more = true;
                break;
            }
        }
        $this->workers[$worker]->collect();
        return $more;
    }

    public function getTaskQueueSizes(): array {
        return array_map(fn(SplQueue $queue) => $queue->count(), $this->taskQueues);
    }

    public function shutdownUnusedWorkers(): int {
        $ret = 0;
        $time = time();
        foreach ($this->taskQueues as $i => $queue) {
            if ((!isset($this->workerLastUsed[$i]) || $this->workerLastUsed[$i] + 300 < $time) && $queue->isEmpty()) {
                $this->workers[$i]->quit();
                $this->eventLoop->removeNotifier($this->workers[$i]->getEntry()->getNotifierId());
                unset($this->workers[$i], $this->taskQueues[$i], $this->workerLastUsed[$i]);
                $ret++;
            }
        }

        return $ret;
    }

    public function tick(int $currentTick): void {
        $this->collectTasks();
    }

    public function shutdown(): void {
        while ($this->collectTasks()) {}
        foreach ($this->workers as $worker) {
            $worker->quit();
            $this->eventLoop->removeNotifier($worker->getEntry()->getNotifierId());
        }
        $this->workers = [];
        $this->taskQueues = [];
        $this->workerLastUsed = [];
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}