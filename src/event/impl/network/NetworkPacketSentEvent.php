<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\Packet;
use pocketcloud\cloud\network\client\ServerClient;

class NetworkPacketSentEvent extends NetworkPacketEvent {

    public function __construct(
        Network $network,
        ServerClient $sender,
        ClientboundPacket $packet,
        protected readonly bool $success
    ) {
        parent::__construct($network, $sender, $packet);
    }

    /** @return ClientboundPacket */
    public function getPacket(): Packet {
        return $this->packet;
    }

    public function isSuccess(): bool {
        return $this->success;
    }
}