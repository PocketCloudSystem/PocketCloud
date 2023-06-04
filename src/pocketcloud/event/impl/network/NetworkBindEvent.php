<?php

namespace pocketcloud\event\impl\network;

use pocketcloud\event\Event;
use pocketcloud\util\Address;

class NetworkBindEvent extends Event {

    public function __construct(private Address $address) {}

    public function getAddress(): Address {
        return $this->address;
    }
}