<?php

namespace pocketcloud\cloud\scheduler;

use pmmp\thread\Runnable;
use pocketcloud\cloud\terminal\log\CloudLogger;
use ReflectionClass;
use Throwable;

abstract class AsyncTask extends Runnable {

    private mixed $result = null;
    private bool $done = false;
    private bool $crashed = false;
    private bool $serialized = false;
    private bool $submitted = false;

    public function run(): void {
        try {
            $this->onRun();
        } catch (Throwable $exception) {
            $this->crashed = true;
            CloudLogger::get()->error("§cAsynchronous task §8'§e" . new ReflectionClass($this)->getShortName() . "§8' §ccrashed!");
            CloudLogger::get()->exception($exception);
        }

        $this->done = true;
    }

    abstract public function onRun(): void;

    public function onCompletion(): void {}

    public function setSubmitted(bool $submitted): void {
        $this->submitted = $submitted;
    }

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

    public function isSubmitted(): bool {
        return $this->submitted;
    }
}