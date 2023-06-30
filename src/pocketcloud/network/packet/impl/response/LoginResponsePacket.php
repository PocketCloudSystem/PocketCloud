<?php

namespace pocketcloud\network\packet\impl\response;

use pocketcloud\network\packet\impl\types\VerifyStatus;
use pocketcloud\network\packet\ResponsePacket;
use pocketcloud\network\packet\utils\PacketData;

class LoginResponsePacket extends ResponsePacket {

    public function __construct(private ?VerifyStatus $status = null) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeVerifyStatus($this->status);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->status = $packetData->readVerifyStatus();
    }

    public function getStatus(): ?VerifyStatus {
        return $this->status;
    }
}