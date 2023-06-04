<?php

namespace pocketcloud\promise;

class Promise {

    private bool $resolved = false;
    private mixed $result = null;
    private ?\Closure $success = null;
    private ?\Closure $failure = null;

    public function resolve(mixed $result) {
        if ($this->resolved) return;
        $this->result = $result;
        if ($this->success !== null) ($this->success)($this->result);
        $this->success = null;
        $this->failure = null;
    }

    public function reject() {
        if ($this->resolved) return;
        if ($this->failure !== null) ($this->failure)();
        $this->success = null;
        $this->failure = null;
    }

    public function then(\Closure $closure): self {
        if ($this->resolved) {
            if ($this->result !== null) ($closure)($this->result);
        } else {
            $this->success = $closure;
        }
        return $this;
    }

    public function failure(\Closure $closure): self {
        if ($this->resolved) {
            if ($this->result === null) ($closure)();
        } else {
            $this->failure = $closure;
        }
        return $this;
    }

    public function isResolved(): bool {
        return $this->resolved;
    }

    public function getResult(): mixed {
        return $this->result;
    }
}