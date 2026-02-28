<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\network\packet\util\PacketData;

/**
 * The normal response packet sent to sub-servers from the cloud after the sub-servers sent a request via RequestPacket
 * @see RequestPacket
 */
abstract class ResponseClientPacket extends CloudPacket implements CloudboundPacket {

    private string $requestId = "";

    final public function encode(PacketData $packetData): void {
        parent::encode($packetData);
        $packetData->write($this->requestId);
    }

    final public function decode(PacketData $packetData): void {
        parent::decode($packetData);
        $this->requestId = $packetData->readString();
    }

    public function getRequestId(): string {
        return $this->requestId;
    }
}