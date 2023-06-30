<?php

namespace pocketcloud\event\impl\network;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;

class NetworkPacketSendEvent extends NetworkEvent {

    public function __construct(
        CloudPacket $packet,
        ServerClient $client,
        private readonly bool $success
    ) {
        parent::__construct($packet, $client);
    }

    public function isSuccess(): bool {
        return $this->success;
    }
}