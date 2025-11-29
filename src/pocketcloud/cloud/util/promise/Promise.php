<?php

namespace pocketcloud\cloud\util\promise;

use Closure;

final class Promise {

    private bool $resolved = false;
    private bool $rejected = false;

    private mixed $result = null;
    private ?Closure $success = null;
    private ?Closure $failure = null;

    public function resolve(mixed $result): void {
        if ($this->resolved) return;
        $this->result = $result;
        $this->resolved = true;
        if ($this->success !== null) ($this->success)($this->result);

        $this->success = null;
        $this->failure = null;
    }

    public function reject(): void {
        if ($this->resolved) return;
        if ($this->failure !== null) ($this->failure)();

        $this->rejected = true;
        $this->success = null;
        $this->failure = null;
    }

    public function then(Closure $closure): self {
        if ($this->resolved) {
            if ($this->result !== null) ($closure)($this->result);
        } else if (!$this->rejected) {
            $this->success = $closure;
        }

        return $this;
    }

    public function failure(Closure $closure): self {
        if ($this->rejected) {
            if ($this->result === null) ($closure)();
        } else if (!$this->resolved) {
            $this->failure = $closure;
        }

        return $this;
    }

    public function isResolved(): bool {
        return $this->resolved;
    }

    public function isRejected(): bool {
        return $this->rejected;
    }

    public function getResult(): mixed {
        return $this->result;
    }
}