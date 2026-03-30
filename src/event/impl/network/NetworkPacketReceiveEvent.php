<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\CloudboundPacket;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;

class NetworkPacketReceiveEvent extends NetworkEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(
        Network $network,
        protected readonly ServerClient $sender,
        protected readonly CloudboundPacket $packet
    ) {
        parent::__construct($network);
    }

    public function getSender(): ServerClient {
        return $this->sender;
    }

    public function getPacket(): CloudboundPacket {
        return $this->packet;
    }
}