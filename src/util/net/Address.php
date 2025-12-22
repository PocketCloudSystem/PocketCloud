<?php

namespace pocketcloud\cloud\util\net;

use pmmp\thread\ThreadSafe;
use pocketcloud\cloud\util\Utils;

final class Address extends ThreadSafe {

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
        return !filter_var($this->address, FILTER_VALIDATE_IP, FILTER_FLAG_NO_PRIV_RANGE | FILTER_FLAG_NO_RES_RANGE);
    }

    public function equals(Address $target): bool {
        return $this->address === $target->getAddress() && $this->port === $target->getPort();
    }

    public static function create(string $address, int $port): self {
        return new self($address, $port);
    }
    
    public static function fromArray(array $data): ?self {
        if (!Utils::containKeys($data, "address", "port")) return null;
        return new self($data["address"], $data["port"]);
    }
}