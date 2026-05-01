<?php

namespace pocketcloud\cloud\exception;

use pocketcloud\cloud\network\packet\ClientboundPacket;

class PacketTooLargeException extends PacketException {

    public function __construct(ClientboundPacket $packet, int $length, int $limit) {
        parent::__construct("Packet {$packet->getName()} is too large: {$length} > {$limit}");
    }
}