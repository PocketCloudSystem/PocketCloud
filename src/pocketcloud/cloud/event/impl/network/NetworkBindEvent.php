<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\event\Event;
use pocketcloud\cloud\util\net\Address;

class NetworkBindEvent extends Event {

    public function __construct(private readonly Address $address) {}

    public function getAddress(): Address {
        return $this->address;
    }
}