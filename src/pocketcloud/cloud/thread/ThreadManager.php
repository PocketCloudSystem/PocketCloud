<?php

namespace pocketcloud\cloud\thread;

use pmmp\thread\ThreadSafe;
use pmmp\thread\ThreadSafeArray;
use pocketcloud\cloud\util\SingletonTrait;
use Throwable;

final class ThreadManager extends ThreadSafe {
    use SingletonTrait;

    private ThreadSafeArray $threads;

    private function __construct() {
        self::setInstance($this);
        $this->threads = new ThreadSafeArray();
    }

    public function add(Worker|Thread $thread): void {
        $this->threads[spl_object_id($thread)] = $thread;
    }

    public function remove(Worker|Thread $thread): void {
        if (isset($this->threads[spl_object_id($this)])) unset($this->threads[spl_object_id($thread)]);
    }

    public function getAll(): array {
        return array_map(function ($thread) {
            return $thread;
        }, (array) $this->threads);
    }

    public function stopAll(): int {
        $crashedThreads = 0;

        foreach ($this->getAll() as $thread) {
            try {
                $thread->quit();
            } catch(Throwable) {
                ++$crashedThreads;
            }
        }

        return $crashedThreads;
    }

    public static function getInstance(): self {
        return self::$instance ??= new self;
    }
}