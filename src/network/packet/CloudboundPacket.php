<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\network\packet\util\PacketData;

/**
 *  CloudboundPacket -> Cloud is the receiver (only decode), Server (Client) is the sender
 */
interface CloudboundPacket extends Packet {

    public function decodePayload(PacketData $packetData): void;
}