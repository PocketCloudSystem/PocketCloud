<?php

namespace pocketcloud\cloud\network\packet\impl\response;

use pocketcloud\cloud\network\packet\data\PacketData;
use pocketcloud\cloud\network\packet\ResponsePacket;

class CheckPlayerNotifyResponsePacket extends ResponsePacket {

    public function __construct(private bool $value = false) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->write($this->value);
    }

    public function decodePayload(PacketData $packetData): void {
        $this->value = $packetData->readBool();
    }

    public function getValue(): bool {
        return $this->value;
    }

    public static function create(bool $value): self {
        return new self($value);
    }
}