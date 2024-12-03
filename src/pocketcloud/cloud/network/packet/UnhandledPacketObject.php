<?php

namespace pocketcloud\cloud\network\packet;

use pmmp\thread\ThreadSafe;

final class UnhandledPacketObject extends ThreadSafe {

    public function __construct(
        private readonly string $buffer,
        private readonly string $address,
        private readonly int $port
    ) {}

    public function getBuffer(): string {
        return $this->buffer;
    }

    public function getAddress(): string {
        return $this->address;
    }

    public function getPort(): int {
        return $this->port;
    }
}