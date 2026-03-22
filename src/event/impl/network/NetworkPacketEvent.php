<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\Packet;
use pocketcloud\cloud\network\client\ServerClient;

abstract class NetworkPacketEvent extends NetworkEvent {

    public function __construct(
        Network $network,
        protected readonly ServerClient $sender,
        protected readonly Packet $packet
    ) {
        parent::__construct($network);
    }

    public function getSender(): ServerClient {
        return $this->sender;
    }

    public function getPacket(): Packet {
        return $this->packet;
    }
}