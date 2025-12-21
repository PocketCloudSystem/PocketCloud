<?php

namespace pocketcloud\cloud\event;

trait CancelableTrait {

    private bool $cancelled = false;

    public function cancel(): void {
        $this->cancelled = true;
    }

    public function uncancel(): void {
        $this->cancelled = false;
    }

    public function isCancelled(): bool {
        return $this->cancelled;
    }
}