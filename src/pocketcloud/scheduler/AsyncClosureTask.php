<?php

namespace pocketcloud\scheduler;

class AsyncClosureTask extends AsyncTask {

    public function __construct(private \Closure $closure, private ?\Closure $completion = null) {}

    public function onRun(): void {
        $reflection = new \ReflectionFunction($this->closure);

        $result = null;
        if ($reflection->getNumberOfParameters() == 0) {
            ($this->closure)();
        } else {
            if ($reflection->getParameters()[0]->isPassedByReference()) ($this->closure)($result);
            else ($this->closure)();
        }

        $this->setResult($result);
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