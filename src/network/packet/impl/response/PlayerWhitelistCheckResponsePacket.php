<?php

namespace pocketcloud\cloud\network\packet\impl\response;

use pocketcloud\cloud\network\packet\ResponsePacket;
use pocketcloud\cloud\network\packet\util\PacketData;

final class PlayerWhitelistCheckResponsePacket extends ResponsePacket {

    public function __construct(private readonly bool $whitelisted = false) {}

    public function encodePayload(PacketData $packetData): void {
        $packetData->writeAll($this->whitelisted);
    }

    public function decodePayload(PacketData $packetData): void {}

    public function isWhitelisted(): bool {
        return $this->whitelisted;
    }

    public static function create(bool $whitelisted): self {
        return new self($whitelisted);
    }
}