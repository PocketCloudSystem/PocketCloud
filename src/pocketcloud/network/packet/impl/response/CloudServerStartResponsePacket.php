<?php

namespace pocketcloud\network\packet\impl\response;

use pocketcloud\network\packet\impl\types\ErrorReason;
use pocketcloud\network\packet\ResponsePacket;
use pocketcloud\network\packet\utils\PacketData;

class CloudServerStartResponsePacket extends ResponsePacket {

    public function __construct(private ?ErrorReason $errorReason = null) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeErrorReason($this->errorReason);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->errorReason = $packetData->readErrorReason();
    }

    public function getErrorReason(): ?ErrorReason {
        return $this->errorReason;
    }
}