<?php

namespace pocketcloud\scheduler;

use Closure;
use JetBrains\PhpStorm\Pure;

class ClosureTask extends Task {

	public function __construct(private readonly Closure $closure) {}
	
	public function onRun(): void {
		($this->closure)($this);
	}

    #[Pure] public static function new(Closure $closure): self {
        return new self($closure);
    }
}