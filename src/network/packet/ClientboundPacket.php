<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\network\packet\util\PacketData;

/**
 *  ClientboundPacket -> Server (Client) is the receiver, Cloud is the sender (only encode)
 */
interface ClientboundPacket extends Packet {

    public function encodePayload(PacketData $packetData): void;
}