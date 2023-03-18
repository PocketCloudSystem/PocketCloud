<?php

namespace pocketcloud\scheduler;

class AsyncClosureTask extends AsyncTask {

    public function __construct(private \Closure $closure, private ?\Closure $completion = null) {}

    public function onRun(): void {
        $this->setResult(($this->closure)($this));
    }

    public function onCompletion(): void {
        if ($this->completion !== null) {
            $reflection = new \ReflectionFunction($this->completion);
            if ($reflection->getNumberOfParameters() == 1) ($this->completion)($this->getResult());
            else ($this->completion)();
        }
    }

    public static function fromClosure(\Closure $closure, ?\Closure $completion = null) {
        return new self($closure, $completion);
    }
}