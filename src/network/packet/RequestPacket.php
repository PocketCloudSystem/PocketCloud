<?php

namespace pocketcloud\cloud\network\packet;

use pocketcloud\cloud\network\client\ServerClient;
use pocketcloud\cloud\network\packet\util\PacketData;

/**
 * The normal request packet sent from sub-servers to the cloud, which will answer through regular ResponsePacket
 * @see ResponsePacket
 */
abstract class RequestPacket extends CloudPacket implements CloudboundPacket {

    private string $requestId = "";

    final public function encode(PacketData $packetData): void {
        parent::encode($packetData);
        $packetData->write($this->requestId);
    }

    final public function decode(PacketData $packetData): void {
        parent::decode($packetData);
        $this->requestId = $packetData->readString();
    }

    public function sendResponse(ResponsePacket $packet, ServerClient $client): void {
        $client->sendPacket($packet->setRequestId($this->requestId));
    }

    public function getRequestId(): string {
        return $this->requestId;
    }
}