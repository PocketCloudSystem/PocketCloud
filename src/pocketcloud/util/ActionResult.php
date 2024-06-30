<?php

namespace pocketcloud\util;

use Closure;

class ActionResult {

    public const SUCCESS = 0;
    private const FAILURE = 1;
    private const WAITING = 2;

    private ?MultipleActionsResult $parent = null;

    public function __construct(
        private int $result,
        private ?Closure $onSuccess = null,
        private ?Closure $onFailure = null,
        private ?string $failureReason = null
    ) {}

    /** @internal */
    public function _setParentMultipleActionsResult(?MultipleActionsResult $parent): void {
        $this->parent = $parent;
        if ($parent !== null && ($this->result == self::SUCCESS || $this->result == self::FAILURE)) {
            $parent->check();
        }
    }

    public function onMarked(Closure $onSuccess, Closure $onFailure): void {
        $this->onSuccess = $onSuccess;
        $this->onFailure = $onFailure;
        if ($this->result == self::SUCCESS) ($this->onSuccess)();
        else if ($this->result == self::FAILURE) ($this->onFailure)($this->failureReason);
    }

    public function markAsSuccess(): void {
        if ($this->result !== self::WAITING) return;
        $this->result = self::SUCCESS;
        if ($this->onSuccess !== null) ($this->onSuccess)();
        $this->parent?->check();
    }

    public function markAsFailure(?string $failureReason = null): void {
        if ($this->result !== self::WAITING) return;
        $this->failureReason = $failureReason;
        $this->result = self::FAILURE;
        if ($this->onFailure !== null) ($this->onFailure)($this->failureReason);
        $this->parent?->check();
    }

    public function wasSuccessful(): int {
        return $this->result == self::SUCCESS;
    }

    public function wasFailure(): int {
        return $this->result == self::FAILURE;
    }

    public function isWaiting(): bool {
        return $this->result == self::WAITING;
    }

    public function getFailureReason(): ?string {
        return $this->failureReason;
    }

    public static function success(): self {
        return new self(self::SUCCESS);
    }

    public static function failure(?string $failureReason = null): self {
        return new self(self::FAILURE, failureReason: $failureReason );
    }

    public static function waiting(): self {
        return new self(self::WAITING);
    }
}