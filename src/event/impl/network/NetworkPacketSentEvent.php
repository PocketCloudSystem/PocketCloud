<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\client\ServerClient;

class NetworkPacketSentEvent extends NetworkEvent {

    public function __construct(
        Network $network,
        protected readonly ServerClient $receiver,
        protected readonly ClientboundPacket $packet,
        protected readonly bool $success
    ) {
        parent::__construct($network);
    }

    public function getReceiver(): ServerClient {
        return $this->receiver;
    }

    public function getPacket(): ClientboundPacket {
        return $this->packet;
    }

    public function isSuccess(): bool {
        return $this->success;
    }
}