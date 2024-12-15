<?php

namespace pocketcloud\cloud\network\client;

use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\server\CloudServer;
use pocketcloud\cloud\terminal\log\CloudLogger;
use pocketcloud\cloud\util\net\Address;
use ReflectionClass;

readonly class ServerClient {

    public function __construct(private Address $address) {}

    public function sendPacket(CloudPacket $packet): bool {
        if (!Network::getInstance()->sendPacket($packet, $this)) {
            CloudLogger::get()->debug("Failed to send packet " . (new ReflectionClass($packet))->getShortName() . " to " . $this->address);
            return false;
        }
        return true;
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public function getServer(): ?CloudServer {
        return ServerClientCache::getInstance()->getServer($this);
    }
}