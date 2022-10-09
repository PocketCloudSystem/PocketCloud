<?php

namespace pocketcloud\scheduler;

class ClosureTask extends Task {

	public function __construct(private \Closure $callable) {}
	
	public function onRun(): void {
		($this->callable)();
	}
}