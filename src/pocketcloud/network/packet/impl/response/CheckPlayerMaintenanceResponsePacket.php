<?php

namespace pocketcloud\network\packet\impl\response;

use pocketcloud\network\packet\ResponsePacket;
use pocketcloud\network\packet\utils\PacketData;

class CheckPlayerMaintenanceResponsePacket extends ResponsePacket {

    public function __construct(private bool $value = false) {}

    public function encodePayload(PacketData $packetData) {
        $packetData->write($this->value);
    }

    public function decodePayload(PacketData $packetData) {
        $this->value = $packetData->readBool();
    }

    public function getValue(): bool {
        return $this->value;
    }
}