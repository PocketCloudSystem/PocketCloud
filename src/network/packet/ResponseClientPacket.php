<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\network\packet\util\PacketData;

/**
 * A different version from the regular ResponsePacket
 * This logic is reversed, means the sub-servers sends this ResponseClientPacket in response to the RequestClientPacket
 * @see RequestClientPacket
 * @see ResponseClientPacket
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

    final public function encodePayload(PacketData $packetData): void {}

    public function getRequestId(): string {
        return $this->requestId;
    }
}