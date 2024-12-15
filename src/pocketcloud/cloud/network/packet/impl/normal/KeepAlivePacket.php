<?php

namespace pocketcloud\cloud\network\packet\impl\normal;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\CloudPacket;

final class KeepAlivePacket extends CloudPacket {

    public function handle(ServerClient $client): void {
        if (($server = $client->getServer()) !== null) {
            $server->setLastCheckTime(time());
            $server->sendPacket(new KeepAlivePacket());
        }
    }
}