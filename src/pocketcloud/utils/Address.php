<?php

namespace pocketcloud\utils;

class Address {

    public function __construct(private string $address, private int $port) {}

    public function getAddress(): string {
        return $this->address;
    }

    public function getPort(): int {
        return $this->port;
    }

    public function __toString(): string {
        return $this->address . ":" . $this->port;
    }
}