<?php

namespace pocketcloud\scheduler;

use pocketcloud\thread\Thread;
use pocketcloud\utils\CloudLogger;

abstract class AsyncTask extends Thread {

    private mixed $result = null;
    private bool $done = false;
    private bool $crashed = false;
    private bool $serialized = false;

    public function run() {
        $this->registerClassLoader();

        try {
            $this->onRun();
        } catch (\Throwable $t) {
            $this->crashed = true;
            CloudLogger::get()->error("§cAsynchron task §8'§e" . (new \ReflectionClass($this))->getShortName() . "§8' §ccrashed!");
            CloudLogger::get()->exception($t);
        }

        $this->done = true;
    }

    abstract public function onRun(): void;

    public function onCompletion(): void {}

    public function setResult(mixed $result): void {
        $this->result = ($this->serialized = !is_scalar($result)) ? igbinary_serialize($result) : $result;
    }

    public function getResult(): mixed {
        if ($this->serialized) return igbinary_unserialize($this->result);
        return $this->result;
    }

    public function hasResult(): bool {
        return $this->result !== null;
    }

    public function isDone(): bool {
        return $this->done || $this->crashed;
    }

    public function isCrashed(): bool {
        return $this->crashed;
    }
}