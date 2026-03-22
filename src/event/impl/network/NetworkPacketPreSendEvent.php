<?php

namespace pocketcloud\cloud\event\impl\network;

use pocketcloud\cloud\network\Network;
use pocketcloud\cloud\network\packet\ClientboundPacket;
use pocketcloud\cloud\network\packet\Packet;
use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\event\Cancelable;
use pocketcloud\cloud\event\CancelableTrait;

class NetworkPacketPreSendEvent extends NetworkPacketEvent implements Cancelable {
    use CancelableTrait;

    public function __construct(
        Network $network,
        ServerClient $sender,
        ClientboundPacket $packet
    ) {
        parent::__construct($network, $sender, $packet);
    }

    /** @return ClientboundPacket */
    public function getPacket(): Packet {
        return $this->packet;
    }
}