<?php

namespace pocketcloud\cloud\network\packet\impl\response;

use pocketcloud\cloud\network\packet\ResponsePacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class PlayerNotificationCheckResponsePacket extends ResponsePacket {

    public function __construct(private readonly bool $enabled = false) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->enabled);
    }

    public function isEnabled(): bool {
        return $this->enabled;
    }

    public static function create(bool $enabled): self {
        return new self($enabled);
    }
}