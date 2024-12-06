<?php

namespace pocketcloud\cloud\util\net;

use pmmp\thread\ThreadSafe;

final  class Address extends ThreadSafe {

    public function __construct(
        private readonly string $address,
        private readonly int $port
    ) {}

    public function getAddress(): string {
        return $this->address;
    }

    public function getPort(): int {
        return $this->port;
    }

    public function __toString(): string {
        return $this->address . ":" . $this->port;
    }

    public function isLocal(): bool {
        return $this->address == "127.0.0.1" || $this->address == "localhost";
    }

    public function equals(Address $target): bool {
        return $this->address === $target->getAddress() && $this->port === $target->getPort();
    }
}