<?php

namespace pocketcloud\event\impl\network;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\CloudPacket;

class NetworkPacketSendEvent extends NetworkEvent {

    public function __construct(private CloudPacket $packet, private ServerClient $client, private bool $success) {
        parent::__construct($this->packet, $this->client);
    }

    public function isSuccess(): bool {
        return $this->success;
    }
}