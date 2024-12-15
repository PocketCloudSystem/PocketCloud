<?php

namespace pocketcloud\cloud\scheduler;

use Closure;

final class ClosureTask extends Task {

	public function __construct(private readonly Closure $closure) {}
	
	public function onRun(): void {
		($this->closure)($this);
	}

    public static function new(Closure $closure): self {
        return new self($closure);
    }
}