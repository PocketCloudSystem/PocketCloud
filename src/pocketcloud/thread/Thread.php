<?php

namespace pocketcloud\thread;

use pocketcloud\exception\ExceptionHandler;

abstract class Thread extends \Thread {

    private bool $running = false;

    public function start(int $options = PTHREADS_INHERIT_NONE): bool {
        ExceptionHandler::set();
        $this->running = true;
        return parent::start($options);
    }

    public function quit() {
        $this->running = false;

        if(!$this->isJoined()){
            $this->notify();
            $this->join();
        }
    }

    public function registerClassLoader() {
        spl_autoload_register(function (string $class): void {
            if (str_starts_with($class, "pocketcloud\\")) {
                if (!class_exists($class)) require str_replace("\\", DIRECTORY_SEPARATOR, __DIR__ . DIRECTORY_SEPARATOR . ".." . DIRECTORY_SEPARATOR . substr($class, strlen("pocketcloud\\"), strlen($class))) . ".php";
            }
        });
    }

    public function isRunning(): bool {
        return true;
    }
}