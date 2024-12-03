<?php

namespace pocketcloud\cloud\event;

interface Cancelable {

    public function cancel(): void;

    public function uncancel(): void;

    public function isCancelled(): bool;
}