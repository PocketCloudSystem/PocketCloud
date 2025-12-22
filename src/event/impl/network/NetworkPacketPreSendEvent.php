<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\ClientboundPacket;

class NetworkPacketPreSendEvent extends NetworkEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(
        private readonly ClientboundPacket $packet,
        ServerClient $client
    ) {
        parent::__construct($client);
    }

    public function getPacket(): ClientboundPacket {
        return $this->packet;
    }
}