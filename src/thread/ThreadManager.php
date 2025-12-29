<?php

namespace pocketcloud\cloud\thread;

use pmmp\thread\ThreadSafe;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\console\log\CloudLogger;
use pocketcloud\cloud\util\trait\SingletonTrait;
use Throwable;

final class ThreadManager extends ThreadSafe {
    use SingletonTrait;

    /** @var ThreadSafeArray<int, Thread|Worker> */
    private ThreadSafeArray $threads;

    public function __construct() {
        self::setInstance($this);
        $this->threads = new ThreadSafeArray();
    }

    public function add(Thread|Worker $thread): void {
        $this->threads[spl_object_id($thread)] = $thread;
    }

    public function remove(Thread|Worker $thread): void {
        if (isset($this->threads[spl_object_id($thread)])) unset($this->threads[spl_object_id($thread)]);
    }

    public function stopAll(): int {
        $crashedThreads = 0;
        $threadIds = array_keys(iterator_to_array($this->threads));

        foreach ($threadIds as $id) {
            if (isset($this->threads[$id])) {
                $thread = $this->threads[$id];
                try {
                    if ($thread->isAlive()) {
                        $thread->quit();
                    }
                } catch (Throwable $exception) {
                    CloudLogger::get()->error("Error while stopping thread: {}", $thread->getThreadName());
                    CloudLogger::get()->exception($exception);
                    $crashedThreads++;
                }
            }
        }

        return $crashedThreads;
    }

    public function count(): int {
        return count($this->threads);
    }

    public function getAll(): array {
        return iterator_to_array($this->threads);
    }
}
