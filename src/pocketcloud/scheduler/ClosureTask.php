<?php

namespace pocketcloud\scheduler;

use JetBrains\PhpStorm\Pure;

class ClosureTask extends Task {

	public function __construct(private \Closure $closure) {}
	
	public function onRun(): void {
		($this->closure)($this);
	}

    #[Pure] public static function new(\Closure $closure) {
        return new self($closure);
    }
}