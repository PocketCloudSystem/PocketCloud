<?php

namespace pocketcloud\network\client;

use pocketcloud\network\Network;
use pocketcloud\network\packet\CloudPacket;
use pocketcloud\server\CloudServer;
use pocketcloud\util\Address;

class ServerClient {

    public function __construct(private readonly Address $address) {}

    public function sendPacket(CloudPacket $packet): bool {
        return Network::getInstance()->sendPacket($packet, $this);
    }

    public function getAddress(): Address {
        return $this->address;
    }

    public function getServer(): ?CloudServer {
        return ServerClientManager::getInstance()->getServerOfClient($this);
    }
}