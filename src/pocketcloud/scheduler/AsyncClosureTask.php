<?php

namespace pocketcloud\scheduler;

use Closure;
use JetBrains\PhpStorm\Pure;

class AsyncClosureTask extends AsyncTask {

    public function __construct(
        private readonly Closure  $closure,
        private readonly ?Closure $completion = null
    ) {}

    public function onRun(): void {
        $this->setResult(($this->closure)($this));
    }

    public function onCompletion(): void {
        if ($this->completion !== null) {
            ($this->completion)($this->getResult());
        }
    }

    #[Pure] public static function new(Closure $closure, ?Closure $completion = null): AsyncClosureTask {
        return new self($closure, $completion);
    }
}