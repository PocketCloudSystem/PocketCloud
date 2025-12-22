<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;

class NetworkPacketSendEvent extends NetworkEvent {

    public function __construct(
        private readonly ClientboundPacket $packet,
        ServerClient $client,
        private readonly bool $success
    ) {
        parent::__construct($client);
    }

    public function getPacket(): ClientboundPacket {
        return $this->packet;
    }

    public function isSuccess(): bool {
        return $this->success;
    }
}