<?php

namespace pocketcloud\util;

use Closure;

class MultipleActionsResult {

    private bool $finished = false;

    public function __construct(
        private array $actionResults = [],
        private ?Closure $onSuccess = null,
        private ?Closure $onFailure = null,
    ) {}

    /** @internal */
    public function check(): void {
        if ($this->isEveryActionSuccessful() || $this->hasEveryActionFailed()) $this->markAsFinished();
    }

    public function markAsFinished(): void {
        if ($this->finished) return;
        $this->finished = true;
        if ($this->isEveryActionSuccessful()) {
            if ($this->onSuccess !== null) ($this->onSuccess)($this->actionResults);
        } else {
            if ($this->onFailure !== null) ($this->onFailure)($this->actionResults);
        }
    }

    public function onFinish(Closure $onSuccess, Closure $onFailure): void {
        $this->onSuccess = $onSuccess;
        $this->onFailure = $onFailure;
        if ($this->isEveryActionSuccessful()) ($this->onSuccess)($this->actionResults);
        else if ($this->hasEveryActionFailed()) ($this->onFailure)($this->actionResults);
    }

    public function addResult(string $key, ActionResult $actionResult): void {
        $this->actionResults[$key] = $actionResult;
        $actionResult->_setParentMultipleActionsResult($this);
    }

    public function getResult(string $key): ?ActionResult {
        return $this->actionResults[$key] ?? null;
    }

    public function getResults(): array {
        return $this->actionResults;
    }

    public function getSuccesses(): array {
        return array_filter($this->actionResults, fn(ActionResult $result) => $result->wasSuccessful());
    }

    public function getFailures(): array {
        return array_filter($this->actionResults, fn(ActionResult $result) => $result->wasFailure());
    }

    public function isEveryActionSuccessful(): bool {
        return count(array_filter($this->actionResults, fn(ActionResult $actionResult) => $actionResult->wasSuccessful())) == count($this->actionResults);
    }

    public function hasEveryActionFailed(): bool {
        return count(array_filter($this->actionResults, fn(ActionResult $actionResult) => $actionResult->wasFailure())) == count($this->actionResults);
    }
}