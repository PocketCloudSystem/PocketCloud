<?php

namespace pocketcloud\event;

interface Cancelable {

    public function cancel(): void;

    public function uncancel(): void;

    public function isCancelled(): bool;
}