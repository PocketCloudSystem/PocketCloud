<?php

namespace pocketcloud\network\packet;

use pocketcloud\network\client\ServerClient;
use pocketcloud\network\packet\utils\PacketData;

abstract class RequestPacket extends CloudPacket {

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