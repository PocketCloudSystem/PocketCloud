<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;

class NetworkPacketPreSendEvent extends NetworkEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(
        Network $network,
        protected readonly ServerClient $receiver,
        protected readonly ClientboundPacket $packet
    ) {
        parent::__construct($network);
    }

    public function getReceiver(): ServerClient {
        return $this->receiver;
    }

    public function getPacket(): ClientboundPacket {
        return $this->packet;
    }
}