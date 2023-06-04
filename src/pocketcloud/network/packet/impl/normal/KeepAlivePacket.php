<?php

namespace pocketcloud\network\packet\impl\normal;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;

class KeepAlivePacket extends CloudPacket {

    public function handle(ServerClient $client) {
        if (($server = $client->getServer()) !== null) {
            $server->setLastCheckTime(time());
            $server->sendPacket(new KeepAlivePacket());
        }
    }
}