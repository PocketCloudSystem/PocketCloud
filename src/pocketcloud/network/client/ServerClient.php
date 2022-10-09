<?php

namespace pocketcloud\network\client;

use pocketcloud\network\Network;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\utils\Address;

class ServerClient {

    public function __construct(private Address $address) {}

    public function sendPacket(CloudPacket $packet): bool {
        return Network::getInstance()->sendPacket($packet, $this);
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public function isLocalHost(): bool {
        $address = $this->address->getAddress();
        return $address == "127.0.0.1" || $address == "0.0.0.0" || $address == "localhost";
    }
}