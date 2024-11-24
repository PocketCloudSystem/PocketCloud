<?php

namespace pocketcloud\network\client;

use pocketcloud\network\Network;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\server\CloudServer;
use pocketcloud\util\Address;
use pocketcloud\util\CloudLogger;

readonly class ServerClient {

    public function __construct(private Address $address) {}

    public function sendPacket(CloudPacket $packet): bool {
        if (!Network::getInstance()->sendPacket($packet, $this)) {
            CloudLogger::get()->debug("Failed to send packet " . (new \ReflectionClass($packet))->getShortName() . " to " . $this->address);
            return false;
        }
        return true;
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public function getServer(): ?CloudServer {
        return ServerClientManager::getInstance()->getServerOfClient($this);
    }
}