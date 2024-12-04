<?php

namespace pocketcloud\cloud\network\packet\impl\response;

use pocketcloud\cloud\config\impl\MainConfig;
use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\network\packet\impl\type\VerifyStatus;
use pocketcloud\cloud\network\packet\ResponsePacket;

final class ServerHandshakeResponsePacket extends ResponsePacket {

    private string $prefix;

    public function __construct(
        private ?VerifyStatus $verifyStatus = null,
    ) {
        $this->prefix = MainConfig::getInstance()->getInGamePrefix();
    }

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeVerifyStatus($this->verifyStatus)
            ->write($this->prefix);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->verifyStatus = $packetData->readVerifyStatus();
        $this->prefix = $packetData->readString();
    }

    public function getVerifyStatus(): ?VerifyStatus {
        return $this->verifyStatus;
    }

    public function getPrefix(): string {
        return $this->prefix;
    }

    public static function create(VerifyStatus $verifyStatus): self {
        return new self($verifyStatus);
    }
}