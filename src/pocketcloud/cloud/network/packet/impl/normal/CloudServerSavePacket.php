<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;
use pocketcloud\cloud\server\CloudServerManager;

class CloudServerSavePacket extends CloudPacket {

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            CloudServerManager::getInstance()->save($server);
        }
    }

    public static function create(): self {
        return new self();
    }
}